package dev.elementary.ability.air;

import dev.elementary.ability.Ability;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** A wind cone that blasts enemies back and rockets the caster away. */
public class Gale implements Ability {
    private final JavaPlugin plugin;

    public Gale(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Gale"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        double length = tier >= 2 ? 12 : 8;
        // fully directional: the cone follows the exact look vector, so
        // aiming down rocket-jumps you and aiming up slams you to earth
        Vector dir = caster.getEyeLocation().getDirection().normalize();
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(length)) {
            if (!dev.elementary.util.Targets.hostile(caster, target)) continue;
            Vector to = target.getEyeLocation().toVector()
                    .subtract(caster.getEyeLocation().toVector());
            if (to.lengthSquared() < 0.01) continue;
            if (to.clone().normalize().angle(dir) > Math.toRadians(30)) continue;
            double push = tier >= 2 ? 2.0 : 1.6;
            target.setVelocity(dir.clone().multiply(push).add(new Vector(0, 0.12, 0)));
            target.damage(2, caster);
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
        }
        // the recoil: launched hard opposite to wherever they aimed
        double recoil = tier >= 2 ? 1.7 : 1.3;
        caster.setVelocity(dir.clone().multiply(-recoil).add(new Vector(0, 0.15, 0)));
        caster.setFallDistance(0);
        for (int ray = -2; ray <= 2; ray++) {
            Vector spread = dir.clone().rotateAroundY(Math.toRadians(ray * 12));
            for (double d = 1; d < length; d += 0.8) {
                org.bukkit.Location at = caster.getEyeLocation().clone()
                        .add(spread.clone().multiply(d));
                caster.getWorld().spawnParticle(Particle.CLOUD, at, 1, 0.1, 0.1, 0.1, 0.02);
                if (ray == 0 && d < 4) {
                    caster.getWorld().spawnParticle(Particle.GUST, at, 1);
                }
            }
        }
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                caster.getLocation().add(0, 1, 0), 1);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_BREEZE_IDLE_AIR, 1.4f, 0.8f);
        return true;
    }
}
