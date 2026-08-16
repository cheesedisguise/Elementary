package dev.elementary.ability.ice;

import dev.elementary.ElementaryPlugin;
import dev.elementary.ability.Ability;
import dev.elementary.tier.Challenges;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Five orbiting ice pellets; click to fire, 2 damage each. */
public class OrbitalIce implements Ability, Listener {
    private final ElementaryPlugin plugin;
    private final Map<UUID, Ring> rings = new HashMap<>();

    public OrbitalIce(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Orbital Ice"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        Ring old = rings.remove(caster.getUniqueId());
        if (old != null) old.dismiss();
        Ring ring = new Ring(caster, tier >= 2 ? 7 : 5);
        rings.put(caster.getUniqueId(), ring);
        ring.runTaskTimer(plugin, 0, 1);
        return true;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getPlayer().isSneaking()) return; // that's the ability-2 input
        fire(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) fire(player);
    }

    private void fire(Player player) {
        Ring ring = rings.get(player.getUniqueId());
        if (ring == null) return;
        ring.fire();
        if (ring.pellets.isEmpty()) {
            ring.dismiss();
            rings.remove(player.getUniqueId());
        }
    }

    private class Ring extends BukkitRunnable {
        final Player owner;
        final List<ItemDisplay> pellets = new ArrayList<>();
        double angle = 0;
        int age = 0;

        Ring(Player owner, int count) {
            this.owner = owner;
            for (int i = 0; i < count; i++) {
                ItemDisplay pellet = owner.getWorld().spawn(owner.getLocation(),
                        ItemDisplay.class, d -> {
                            d.setItemStack(new ItemStack(Material.PACKED_ICE));
                            d.setTransformation(new Transformation(new Vector3f(),
                                    new Quaternionf(), new Vector3f(0.35f, 0.35f, 0.35f),
                                    new Quaternionf()));
                            d.setPersistent(false);
                        });
                pellets.add(pellet);
            }
        }

        void dismiss() {
            pellets.forEach(ItemDisplay::remove);
            pellets.clear();
            cancel();
        }

        void fire() {
            if (pellets.isEmpty()) return;
            ItemDisplay pellet = pellets.remove(0);
            Vector dir = owner.getEyeLocation().getDirection().normalize();
            Location start = owner.getEyeLocation().clone().add(dir.clone().multiply(0.8));
            owner.getWorld().playSound(owner.getLocation(),
                    org.bukkit.Sound.BLOCK_GLASS_BREAK, 0.7f, 1.8f);
            new BukkitRunnable() {
                final Location at = start.clone();
                double flown = 0;
                @Override public void run() {
                    for (int step = 0; step < 2; step++) {
                        at.add(dir.clone().multiply(0.6));
                        flown += 0.6;
                        at.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 2,
                                0.05, 0.05, 0.05, 0.01);
                        for (LivingEntity target : at.getNearbyLivingEntities(0.9)) {
                            if (!dev.elementary.util.Targets.hostile(owner, target)) continue;
                            // honest damage now - armour applies, no true bypass
                            target.damage(2, owner);
                            Challenges.pelletHit(plugin, owner);
                            impact(at);
                            pellet.remove();
                            cancel();
                            return;
                        }
                        if (at.getBlock().getType().isSolid() || flown >= 24) {
                            impact(at);
                            pellet.remove();
                            cancel();
                            return;
                        }
                    }
                    pellet.teleport(at);
                }
            }.runTaskTimer(plugin, 0, 1);
        }

        void impact(Location at) {
            at.getWorld().spawnParticle(Particle.BLOCK, at, 14, 0.2, 0.2, 0.2,
                    Material.ICE.createBlockData());
            at.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 10, 0.25, 0.25, 0.25, 0.03);
            at.getWorld().playSound(at, org.bukkit.Sound.BLOCK_GLASS_BREAK, 0.6f, 1.4f);
        }

        @Override
        public void run() {
            age++;
            if (age > 20 * 20 || !owner.isOnline() || pellets.isEmpty()) {
                dismiss();
                rings.remove(owner.getUniqueId(), this);
                return;
            }
            angle += Math.PI / 20; // one revolution every ~2s
            for (int i = 0; i < pellets.size(); i++) {
                double a = angle + 2 * Math.PI * i / pellets.size();
                Location at = owner.getLocation().clone().add(
                        1.5 * Math.cos(a),
                        1.2 + 0.15 * Math.sin(age / 6.0 + i),
                        1.5 * Math.sin(a));
                pellets.get(i).teleport(at);
                if (age % 3 == 0) {
                    owner.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 1,
                            0.02, 0.02, 0.02, 0.002);
                }
            }
        }
    }
}
