package dev.elementary.ability.shadow;

import dev.elementary.ElementaryPlugin;
import dev.elementary.status.StatusService;
import dev.elementary.util.Targets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Hunt: for twenty seconds a 35-block storm of dread follows the
 * caster. Every enemy inside is held in Fear, the sky turns to
 * midnight on their screens (and scrolls back when they escape), the
 * caster's heartbeat pounds in their ears, and the caster hits
 * everyone inside 10% harder. The hunter himself is FULLY invisible
 * for the duration - armour, held items, everything - and all that
 * remains of him is a red glow outline stalking through the dark
 * (Glowing on an invisible player renders only the outline; a red
 * scoreboard team colours it).
 */
public class Hunt implements dev.elementary.ability.Ability {
    public static final double RADIUS = 35;
    private static final int DURATION = 20 * 20;
    private static final String TEAM = "elemHuntGlow";
    private static final org.bukkit.inventory.EquipmentSlot[] VISIBLE_SLOTS = {
            org.bukkit.inventory.EquipmentSlot.HAND,
            org.bukkit.inventory.EquipmentSlot.OFF_HAND,
            org.bukkit.inventory.EquipmentSlot.HEAD,
            org.bukkit.inventory.EquipmentSlot.CHEST,
            org.bukkit.inventory.EquipmentSlot.LEGS,
            org.bukkit.inventory.EquipmentSlot.FEET};

    /** caster -> tick the hunt ends. Consulted by CombatListener. */
    private static final Map<UUID, Integer> hunts = new HashMap<>();
    /** players whose sky we darkened, per hunting caster. */
    private static final Map<UUID, Set<UUID>> nightbound = new HashMap<>();
    /** caster names, for stripping the glow team after they log off. */
    private static final Map<UUID, String> hunterNames = new HashMap<>();

    private final ElementaryPlugin plugin;

    public Hunt(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Hunt"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    /** +10% from the hunter against anyone inside their storm. */
    public static double damageMultiplier(Player damager, LivingEntity victim) {
        Integer end = hunts.get(damager.getUniqueId());
        if (end == null || Bukkit.getCurrentTick() >= end) return 1.0;
        if (!victim.getWorld().equals(damager.getWorld())) return 1.0;
        return victim.getLocation().distanceSquared(damager.getLocation())
                <= RADIUS * RADIUS ? 1.10 : 1.0;
    }

    @Override
    public boolean cast(Player caster, int tier) {
        int end = Bukkit.getCurrentTick() + DURATION;
        hunts.put(caster.getUniqueId(), end);
        nightbound.put(caster.getUniqueId(), new HashSet<>());
        hunterNames.put(caster.getUniqueId(), caster.getName());
        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
        caster.getWorld().playSound(caster.getLocation(),
                Sound.AMBIENT_CAVE, 1.4f, 0.6f);
        // the hunter dissolves: only a red outline remains
        caster.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY,
                DURATION + 10, 0, true, false, false));
        caster.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING,
                DURATION + 10, 0, true, false, false));
        redTeam().addEntry(caster.getName());
        cloak(caster);
        new BukkitRunnable() {
            @Override public void run() {
                int now = Bukkit.getCurrentTick();
                Set<UUID> darkened = nightbound.get(caster.getUniqueId());
                if (now >= end || !caster.isOnline() || caster.isDead()
                        || darkened == null) {
                    endHunt(caster.getUniqueId());
                    cancel();
                    return;
                }
                // equipment packets from item swaps re-reveal gear;
                // re-hide it every second
                if (now % 20 < 10) cloak(caster);
                ambience(caster, now);
                Set<UUID> stillInside = new HashSet<>();
                for (LivingEntity target : caster.getLocation()
                        .getNearbyLivingEntities(RADIUS)) {
                    if (!Targets.hostile(caster, target)) continue;
                    if (target.getLocation().distanceSquared(caster.getLocation())
                            > RADIUS * RADIUS) continue;
                    if (now % 20 < 10) {
                        plugin.status().apply(target, StatusService.Status.FEAR,
                                30, caster);
                    }
                    if (target instanceof Player inside) {
                        stillInside.add(inside.getUniqueId());
                        if (darkened.add(inside.getUniqueId())) {
                            inside.setPlayerTime(18000, false); // midnight
                        }
                        if (now % 40 < 10) {
                            // the caster's heartbeat, in their ears
                            inside.playSound(inside.getLocation(),
                                    Sound.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.85f);
                        }
                    }
                }
                // escaped the storm: the sky scrolls back
                darkened.removeIf(id -> {
                    if (stillInside.contains(id)) return false;
                    Player freed = Bukkit.getPlayer(id);
                    if (freed != null) freed.resetPlayerTime();
                    return true;
                });
            }
        }.runTaskTimer(plugin, 0, 10);
        return true;
    }

    private void ambience(Player caster, int now) {
        Location base = caster.getLocation();
        // sparse ink motes drifting through the whole storm volume
        for (int i = 0; i < 6; i++) {
            double a = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double r = 4 + ThreadLocalRandom.current().nextDouble() * (RADIUS - 5);
            base.getWorld().spawnParticle(Particle.SQUID_INK,
                    base.clone().add(r * Math.cos(a),
                            0.5 + ThreadLocalRandom.current().nextDouble() * 3,
                            r * Math.sin(a)),
                    1, 0.2, 0.2, 0.2, 0.002);
        }
        for (int i = 0; i < 10; i++) {
            double a = now * 0.02 + 2 * Math.PI * i / 10;
            base.getWorld().spawnParticle(Particle.DUST,
                    base.clone().add(RADIUS * Math.cos(a),
                            1 + (i % 3), RADIUS * Math.sin(a)), 1, 0.4, 0.5, 0.4, 0,
                    new Particle.DustOptions(Color.fromRGB(0x4D0F18), 1.6f));
        }
        if (now % 100 < 10) {
            base.getWorld().playSound(base, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
                    0.8f, 0.5f);
        }
    }

    private static void endHunt(UUID casterId) {
        hunts.remove(casterId);
        Player caster = Bukkit.getPlayer(casterId);
        if (caster != null && caster.isOnline()) {
            caster.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            caster.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
            uncloak(caster);
        }
        String name = hunterNames.remove(casterId);
        if (name != null) {
            org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager()
                    .getMainScoreboard().getTeam(TEAM);
            if (team != null) team.removeEntry(name);
        }
        Set<UUID> darkened = nightbound.remove(casterId);
        if (darkened == null) return;
        for (UUID id : darkened) {
            Player freed = Bukkit.getPlayer(id);
            if (freed != null) freed.resetPlayerTime();
        }
    }

    /** The red-glow team; registered once, reused for every hunt. */
    private static org.bukkit.scoreboard.Team redTeam() {
        org.bukkit.scoreboard.Scoreboard board =
                Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = board.getTeam(TEAM);
        if (team == null) {
            team = board.registerNewTeam(TEAM);
            team.color(net.kyori.adventure.text.format.NamedTextColor.RED);
        }
        return team;
    }

    /** Tell every other client the hunter wears and holds nothing. */
    private static void cloak(Player caster) {
        Map<org.bukkit.inventory.EquipmentSlot, org.bukkit.inventory.ItemStack> bare =
                new java.util.EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
        for (org.bukkit.inventory.EquipmentSlot slot : VISIBLE_SLOTS) {
            bare.put(slot, new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
        }
        for (Player viewer : caster.getWorld().getPlayers()) {
            if (!viewer.equals(caster)) viewer.sendEquipmentChange(caster, bare);
        }
    }

    /** The hunt ends: everyone sees the real gear again. */
    private static void uncloak(Player caster) {
        org.bukkit.inventory.PlayerInventory inv = caster.getInventory();
        Map<org.bukkit.inventory.EquipmentSlot, org.bukkit.inventory.ItemStack> real =
                new java.util.EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
        for (org.bukkit.inventory.EquipmentSlot slot : VISIBLE_SLOTS) {
            org.bukkit.inventory.ItemStack item = inv.getItem(slot);
            real.put(slot, item != null ? item
                    : new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
        }
        for (Player viewer : caster.getWorld().getPlayers()) {
            if (!viewer.equals(caster)) viewer.sendEquipmentChange(caster, real);
        }
    }

    /** Plugin shutdown: give every darkened player their sky back. */
    public static void cleanupAll() {
        for (UUID casterId : new HashSet<>(nightbound.keySet())) {
            endHunt(casterId);
        }
        hunts.clear();
    }
}
