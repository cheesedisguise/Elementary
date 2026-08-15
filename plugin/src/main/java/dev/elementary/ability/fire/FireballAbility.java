package dev.elementary.ability.fire;

import dev.elementary.ability.Ability;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireballAbility implements Ability, Listener {
    private static final String TAG = "elementary_fireball";

    private final JavaPlugin plugin;

    public FireballAbility(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Fireball"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        int count = tier >= 2 ? 3 : 1;
        for (int i = 0; i < count; i++) {
            double spread = (i - (count - 1) / 2.0) * Math.toRadians(12);
            Vector dir = caster.getEyeLocation().getDirection().rotateAroundY(spread);
            Fireball ball = caster.launchProjectile(Fireball.class, dir.multiply(1.6));
            ball.setYield(0f);
            ball.setIsIncendiary(false);
            ball.setMetadata(TAG, new FixedMetadataValue(plugin, true));
            trail(ball);
        }
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1f, 0.9f);
        return true;
    }

    private void trail(Fireball ball) {
        new BukkitRunnable() {
            @Override public void run() {
                if (ball.isDead()) { cancel(); return; }
                ball.getWorld().spawnParticle(Particle.FLAME, ball.getLocation(), 4,
                        0.08, 0.08, 0.08, 0.01);
                ball.getWorld().spawnParticle(Particle.LAVA, ball.getLocation(), 1,
                        0.05, 0.05, 0.05, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.hasMetadata(TAG)) return;
        event.setCancelled(true); // no vanilla explosion at all
        projectile.remove();
        org.bukkit.Location at = projectile.getLocation();
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().spawnParticle(Particle.DUST, at, 40, 1.2, 1.2, 1.2, 0,
                new Particle.DustOptions(Color.fromRGB(0xF07020), 1.2f));
        at.getWorld().playSound(at, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.1f);
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        for (LivingEntity target : at.getNearbyLivingEntities(1.5 + 1.0)) {
            if (shooter != null && !dev.elementary.util.Targets.hostile(shooter, target)) {
                continue;
            }
            target.damage(6, shooter);
            target.setFireTicks(Math.max(target.getFireTicks(), 60));
        }
    }
}
