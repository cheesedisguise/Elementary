package dev.elementary.status;

import dev.elementary.ElementaryPlugin;
import dev.elementary.element.Element;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Elementary's own status effects. Each one is a bundle: our timers and
 * damage/speed math here, plus hidden vanilla effects (ambient, no
 * particles, no HUD icon) so the client renders the right screen state
 * while our font HUD shows the single status icon.
 *
 * <ul>
 *   <li><b>Luminosity</b> - overexposed: -20% damage dealt, -15% speed.
 *       Light is dimmer after dark: the debuff is 60% weaker at night.
 *   <li><b>Absolute Radiance</b> - Luminosity gone supernova: -35%
 *       damage, -30% speed, glowing, blurry screen, a burn no fire
 *       resistance can refuse, and Light moves hit the bearer 50%
 *       harder. 40% weaker at night; gaining darkness snuffs it out.
 *   <li><b>Harmony</b> - +15% speed, +15% damage, rainbow trail.
 *   <li><b>Concussion</b> - the screen swims (nausea) and fades back
 *       over a second once it ends.
 *   <li><b>Fear</b> - +15% damage taken, blindness and darkness
 *       bundled, hearts turn shadow-black (a wither shade kept too
 *       short to ever deal its damage), and the feared one sees their
 *       tormentor as a red silhouette through anything.
 * </ul>
 */
public class StatusService extends BukkitRunnable implements org.bukkit.event.Listener {
    public enum Status {
        LUMINOSITY("Luminosity"),
        ABSOLUTE_RADIANCE("Absolute Radiance"),
        HARMONY("Harmony"),
        CONCUSSION("Concussion"),
        FEAR("Fear");

        private final String display;
        Status(String display) { this.display = display; }
        public String display() { return display; }
    }

    private static final class Instance {
        int endTick;
        UUID applier;
        Instance(int endTick, UUID applier) { this.endTick = endTick; this.applier = applier; }
    }

    private final ElementaryPlugin plugin;
    private final NamespacedKey speedKey;
    private final Map<UUID, EnumMap<Status, Instance>> statuses = new HashMap<>();

    public StatusService(ElementaryPlugin plugin) {
        this.plugin = plugin;
        this.speedKey = new NamespacedKey(plugin, "status_speed");
    }

    // ------------------------------------------------------------- API

    /** Applies (or extends) a status. Reapplying never shortens it. */
    public void apply(LivingEntity target, Status status, int ticks, Player applier) {
        if (target.isDead()) return;
        if (target instanceof Player p && p.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        EnumMap<Status, Instance> mine =
                statuses.computeIfAbsent(target.getUniqueId(), k -> new EnumMap<>(Status.class));
        // Absolute Radiance REPLACES Luminosity, never stacks beside it
        if (status == Status.ABSOLUTE_RADIANCE) mine.remove(Status.LUMINOSITY);
        if (status == Status.LUMINOSITY && mine.containsKey(Status.ABSOLUTE_RADIANCE)) {
            return; // already carrying the stronger form
        }
        int end = Bukkit.getCurrentTick() + ticks;
        Instance cur = mine.get(status);
        boolean fresh = cur == null;
        if (fresh) {
            mine.put(status, new Instance(end, applier == null ? null : applier.getUniqueId()));
        } else {
            cur.endTick = Math.max(cur.endTick, end);
            if (applier != null) cur.applier = applier.getUniqueId();
        }
        if (fresh) onApply(target, status);
        bundle(target, mine);
    }

    public boolean has(LivingEntity target, Status status) {
        EnumMap<Status, Instance> mine = statuses.get(target.getUniqueId());
        return mine != null && mine.containsKey(status);
    }

    /** Active statuses, in enum order - the HUD's status row. */
    public Set<Status> active(UUID id) {
        EnumMap<Status, Instance> mine = statuses.get(id);
        return mine == null ? EnumSet.noneOf(Status.class) : EnumSet.copyOf(mine.keySet());
    }

    public void remove(LivingEntity target, Status status) {
        EnumMap<Status, Instance> mine = statuses.get(target.getUniqueId());
        if (mine == null || mine.remove(status) == null) return;
        onExpire(target, status, mine);
        if (mine.isEmpty()) statuses.remove(target.getUniqueId());
        speed(target, mine);
    }

    /** Everything the bearer deals is scaled by what rides them. */
    public double damageOutMultiplier(Player damager) {
        EnumMap<Status, Instance> mine = statuses.get(damager.getUniqueId());
        if (mine == null) return 1.0;
        boolean night = !damager.getWorld().isDayTime();
        double m = 1.0;
        if (mine.containsKey(Status.LUMINOSITY)) m *= night ? 0.92 : 0.80;
        if (mine.containsKey(Status.ABSOLUTE_RADIANCE)) m *= night ? 0.79 : 0.65;
        if (mine.containsKey(Status.HARMONY)) m *= 1.15;
        return m;
    }

    /** Everything the bearer takes is scaled too (Fear; Radiance vs Light). */
    public double damageInMultiplier(LivingEntity victim, Player damager) {
        EnumMap<Status, Instance> mine = statuses.get(victim.getUniqueId());
        if (mine == null) return 1.0;
        double m = 1.0;
        if (mine.containsKey(Status.FEAR)) m *= 1.15;
        if (mine.containsKey(Status.ABSOLUTE_RADIANCE) && damager != null
                && plugin.shards().dataFor(damager).element == Element.LIGHT) {
            m *= 1.5; // absolute radiance is a lens - Light pours through
        }
        return m;
    }

    // ------------------------------------------------------------ tick

    @Override
    public void run() {
        int now = Bukkit.getCurrentTick();
        Iterator<Map.Entry<UUID, EnumMap<Status, Instance>>> it =
                statuses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EnumMap<Status, Instance>> entry = it.next();
            Entity raw = Bukkit.getEntity(entry.getKey());
            EnumMap<Status, Instance> mine = entry.getValue();
            if (!(raw instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
                it.remove();
                continue;
            }
            // darkness snuffs out Absolute Radiance - Fear's bundled
            // darkness counts too: shadow smothering light is the point
            if (mine.containsKey(Status.ABSOLUTE_RADIANCE)
                    && target.hasPotionEffect(PotionEffectType.DARKNESS)) {
                mine.remove(Status.ABSOLUTE_RADIANCE);
                onExpire(target, Status.ABSOLUTE_RADIANCE, mine);
                target.getWorld().playSound(target.getLocation(),
                        Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.4f);
            }
            // natural expiry
            for (Iterator<Map.Entry<Status, Instance>> si = mine.entrySet().iterator();
                    si.hasNext(); ) {
                Map.Entry<Status, Instance> se = si.next();
                if (now >= se.getValue().endTick) {
                    si.remove();
                    onExpire(target, se.getKey(), mine);
                }
            }
            if (mine.isEmpty()) {
                it.remove();
                speed(target, mine);
                continue;
            }
            bundle(target, mine);
            particles(target, mine, now);
            burn(target, mine, now);
            reveal(target, mine, now);
            speed(target, mine);
        }
    }

    private void onApply(LivingEntity target, Status status) {
        switch (status) {
            case FEAR -> target.getWorld().playSound(target.getLocation(),
                    Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.35f, 1.8f);
            case LUMINOSITY -> target.getWorld().playSound(target.getLocation(),
                    Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
            case ABSOLUTE_RADIANCE -> {
                target.getWorld().playSound(target.getLocation(),
                        Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.9f);
                target.getWorld().spawnParticle(Particle.FLASH,
                        target.getLocation().add(0, 1, 0), 1);
            }
            case HARMONY -> target.getWorld().playSound(target.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.3f);
            case CONCUSSION -> target.getWorld().playSound(target.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 0.5f);
        }
    }

    /** Hidden vanilla effects kept in step with our own timers. */
    private void bundle(LivingEntity target, EnumMap<Status, Instance> mine) {
        int now = Bukkit.getCurrentTick();
        Instance fear = mine.get(Status.FEAR);
        if (fear != null) {
            int rem = Math.max(1, fear.endTick - now);
            hidden(target, PotionEffectType.BLINDNESS, rem, 0);
            hidden(target, PotionEffectType.DARKNESS, rem, 0);
            // shadow-black hearts: wither at amp 0 only deals damage
            // when its remaining time crosses a multiple of 40 ticks -
            // capped at 35 and refreshed, it can never hurt, only stain
            hidden(target, PotionEffectType.WITHER, Math.min(rem, 35), 0);
        }
        Instance ar = mine.get(Status.ABSOLUTE_RADIANCE);
        if (ar != null) {
            int rem = Math.max(1, ar.endTick - now);
            hidden(target, PotionEffectType.GLOWING, rem, 0);
            hidden(target, PotionEffectType.NAUSEA, Math.min(rem + 20, 140), 0);
        }
        Instance conc = mine.get(Status.CONCUSSION);
        if (conc != null) {
            int rem = Math.max(1, conc.endTick - now);
            // the +25 tick tail is the "fades out over a second"
            hidden(target, PotionEffectType.NAUSEA, Math.min(rem + 25, 140), 0);
        }
    }

    private void hidden(LivingEntity target, PotionEffectType type, int ticks, int amp) {
        target.addPotionEffect(new PotionEffect(type, ticks, amp, true, false, false));
    }

    private void onExpire(LivingEntity target, Status status, EnumMap<Status, Instance> left) {
        switch (status) {
            case FEAR -> {
                target.removePotionEffect(PotionEffectType.BLINDNESS);
                target.removePotionEffect(PotionEffectType.DARKNESS);
                target.removePotionEffect(PotionEffectType.WITHER);
            }
            case ABSOLUTE_RADIANCE -> {
                target.removePotionEffect(PotionEffectType.GLOWING);
                // nausea is left to run out its short tail (the fade)
            }
            default -> { }
        }
    }

    private void particles(LivingEntity target, EnumMap<Status, Instance> mine, int now) {
        org.bukkit.Location at = target.getLocation();
        if (mine.containsKey(Status.HARMONY) && now % 4 == 0) {
            float hue = (now % 60) / 60f;
            java.awt.Color c = java.awt.Color.getHSBColor(hue, 0.75f, 1f);
            at.getWorld().spawnParticle(Particle.DUST,
                    at.clone().add(0, 1.1, 0), 2, 0.35, 0.5, 0.35, 0,
                    new Particle.DustOptions(Color.fromRGB(
                            c.getRed(), c.getGreen(), c.getBlue()), 1.1f));
        }
        if (mine.containsKey(Status.LUMINOSITY) && now % 8 == 0) {
            at.getWorld().spawnParticle(Particle.DUST,
                    at.clone().add(0, 1.4, 0), 2, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(0xFFE08A), 1.0f));
        }
        if (mine.containsKey(Status.ABSOLUTE_RADIANCE) && now % 4 == 0) {
            at.getWorld().spawnParticle(Particle.END_ROD,
                    at.clone().add(0, 1.2, 0), 2, 0.35, 0.6, 0.35, 0.015);
        }
        if (mine.containsKey(Status.FEAR) && now % 10 == 0) {
            at.getWorld().spawnParticle(Particle.SMOKE,
                    at.clone().add(0, 1.6, 0), 3, 0.25, 0.3, 0.25, 0.01);
        }
    }

    /** Absolute Radiance burns - and no fire resistance is consulted. */
    private void burn(LivingEntity target, EnumMap<Status, Instance> mine, int now) {
        if (!mine.containsKey(Status.ABSOLUTE_RADIANCE) || now % 20 != 0) return;
        target.getWorld().spawnParticle(Particle.FLAME,
                target.getLocation().add(0, 1, 0), 6, 0.25, 0.5, 0.25, 0.01);
        // flat one-per-second, deliberately outside the multiplier math
        if (target.isDead() || target.isInvulnerable()) return;
        if (target instanceof Player p && p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        Instance ar = mine.get(Status.ABSOLUTE_RADIANCE);
        Player src = ar.applier == null ? null : Bukkit.getPlayer(ar.applier);
        double health = target.getHealth() - 1.0;
        if (health <= 0) {
            target.setNoDamageTicks(0);
            target.damage(Math.max(1.0, target.getHealth()), src);
        } else {
            target.setHealth(health);
            target.playHurtAnimation(0);
        }
    }

    /** The feared see their tormentor: a red silhouette through walls,
     *  invisibility, everything - drawn only on the victim's client. */
    private void reveal(LivingEntity target, EnumMap<Status, Instance> mine, int now) {
        Instance fear = mine.get(Status.FEAR);
        if (fear == null || fear.applier == null || now % 6 != 0) return;
        if (!(target instanceof Player victim)) return;
        Player applier = Bukkit.getPlayer(fear.applier);
        if (applier == null || !applier.getWorld().equals(victim.getWorld())) return;
        if (applier.getLocation().distanceSquared(victim.getLocation()) > 60 * 60) return;
        org.bukkit.Location base = applier.getLocation();
        for (double y = 0.2; y <= 1.8; y += 0.4) {
            victim.spawnParticle(Particle.DUST, base.clone().add(0, y, 0), 1,
                    0.12, 0.1, 0.12, 0, new Particle.DustOptions(
                            Color.fromRGB(0xC0404E), 1.4f));
        }
    }

    /** One combined MULTIPLY modifier carries every speed change. */
    private void speed(LivingEntity target, EnumMap<Status, Instance> mine) {
        AttributeInstance attr = target.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        boolean night = !target.getWorld().isDayTime();
        double m = 1.0;
        if (mine.containsKey(Status.LUMINOSITY)) m *= 1 - (night ? 0.06 : 0.15);
        if (mine.containsKey(Status.ABSOLUTE_RADIANCE)) m *= 1 - (night ? 0.18 : 0.30);
        if (mine.containsKey(Status.HARMONY)) m *= 1.15;
        double amount = m - 1.0;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (mod.getKey().equals(speedKey)) {
                if (Math.abs(mod.getAmount() - amount) < 0.0001) return;
                attr.removeModifier(mod);
            }
        }
        if (Math.abs(amount) > 0.0001) {
            attr.addModifier(new AttributeModifier(speedKey, amount,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    // ------------------------------------------------------- lifecycle

    @org.bukkit.event.EventHandler
    public void onDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        statuses.remove(event.getEntity().getUniqueId());
    }

    /** A stale modifier must never survive a relog. */
    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        statuses.remove(event.getPlayer().getUniqueId());
        speed(event.getPlayer(), new EnumMap<>(Status.class));
    }

    /** Plugin shutdown: strip every bundle and modifier we own. */
    public void shutdown() {
        for (Map.Entry<UUID, EnumMap<Status, Instance>> entry : statuses.entrySet()) {
            if (Bukkit.getEntity(entry.getKey()) instanceof LivingEntity target) {
                for (Status status : Status.values()) {
                    if (entry.getValue().containsKey(status)) {
                        onExpire(target, status, entry.getValue());
                    }
                }
                target.removePotionEffect(PotionEffectType.NAUSEA);
                speed(target, new EnumMap<>(Status.class));
            }
        }
        statuses.clear();
    }
}
