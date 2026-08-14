package dev.elementary.ability.earth;

import dev.elementary.ability.Ability;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** An orbiting 5x3 stone wall; punch it to send it flying. */
public class Bulwark implements Ability, Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Wall> walls = new HashMap<>();

    public Bulwark(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Bulwark"; }
    @Override public double cooldownSeconds(int tier) { return 35; }

    @Override
    public boolean cast(Player caster, int tier) {
        Wall existing = walls.remove(caster.getUniqueId());
        if (existing != null) {
            existing.dismiss();
            return false; // recast dismisses without recharging
        }
        Wall wall = new Wall(caster, tier);
        walls.put(caster.getUniqueId(), wall);
        wall.runTaskTimer(plugin, 0, 1);
        return true;
    }

    @EventHandler
    public void onPunch(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Wall wall = walls.get(event.getPlayer().getUniqueId());
        if (wall != null && !wall.launched && wall.centerDistance(event.getPlayer()) < 5.5) {
            wall.launch();
        }
    }

    private class Wall extends BukkitRunnable {
        final Player owner;
        final int tier;
        final List<BlockDisplay> blocks = new ArrayList<>();
        final List<UUID> shoved = new ArrayList<>();
        Location center;
        Vector direction;
        boolean launched = false;
        double travelled = 0;
        int age = 0;

        Wall(Player owner, int tier) {
            this.owner = owner;
            this.tier = tier;
            this.center = anchor();
            for (int i = 0; i < 15; i++) {
                BlockDisplay display = owner.getWorld().spawn(center, BlockDisplay.class, d -> {
                    d.setBlock(Material.STONE_BRICKS.createBlockData());
                    d.setPersistent(false);
                });
                blocks.add(display);
            }
            layout();
        }

        Location anchor() {
            Vector look = owner.getLocation().getDirection().setY(0);
            if (look.lengthSquared() < 0.01) look = new Vector(1, 0, 0);
            return owner.getLocation().add(look.normalize().multiply(3));
        }

        double centerDistance(Player player) {
            return player.getLocation().distance(center);
        }

        void layout() {
            Vector look = center.toVector().subtract(owner.getLocation().toVector()).setY(0);
            if (look.lengthSquared() < 0.01) look = new Vector(1, 0, 0);
            look.normalize();
            Vector side = new Vector(-look.getZ(), 0, look.getX());
            int i = 0;
            for (int col = -2; col <= 2; col++) {
                for (int row = 0; row < 3; row++) {
                    Location at = center.clone()
                            .add(side.clone().multiply(col))
                            .add(0, row, 0);
                    blocks.get(i++).teleport(at.setDirection(look));
                }
            }
        }

        void launch() {
            launched = true;
            direction = center.toVector().subtract(owner.getLocation().toVector()).setY(0)
                    .normalize();
            owner.getWorld().playSound(center, org.bukkit.Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
        }

        void dismiss() {
            blocks.forEach(BlockDisplay::remove);
            cancel();
            walls.remove(owner.getUniqueId(), this);
        }

        @Override
        public void run() {
            age++;
            if (age > 20 * 12 || !owner.isOnline()) { dismiss(); return; }
            if (!launched) {
                center = anchor();
                layout();
                if (age % 8 == 0) {
                    owner.getWorld().spawnParticle(Particle.BLOCK, center, 4, 2, 1, 2,
                            Material.STONE_BRICKS.createBlockData());
                }
                return;
            }
            double step = 0.4; // 8 blocks/second
            center.add(direction.clone().multiply(step));
            travelled += step;
            layout();
            owner.getWorld().spawnParticle(Particle.CRIT, center, 6, 2, 1, 0.3, 0.05);
            owner.getWorld().spawnParticle(Particle.BLOCK, center, 8, 2, 1, 0.3,
                    Material.STONE_BRICKS.createBlockData());
            for (LivingEntity target : center.getNearbyLivingEntities(2.6, 1.8, 2.6)) {
                if (target.equals(owner)) continue;
                target.setVelocity(direction.clone().multiply(1.1).setY(0.25));
                if (!shoved.contains(target.getUniqueId())) {
                    shoved.add(target.getUniqueId());
                    target.damage(tier >= 2 ? 6 : 3, owner);
                }
            }
            boolean hitTerrain = center.getBlock().getType().isSolid();
            double maxRange = tier >= 2 ? 25 : 15;
            if (hitTerrain || travelled >= maxRange) {
                owner.getWorld().spawnParticle(Particle.BLOCK, center, 40, 2, 1.5, 2,
                        Material.STONE_BRICKS.createBlockData());
                dismiss();
            }
        }
    }
}
