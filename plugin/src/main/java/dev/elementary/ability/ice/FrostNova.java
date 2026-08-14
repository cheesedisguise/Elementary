package dev.elementary.ability.ice;

import dev.elementary.ability.Ability;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Frost Nova: a ring of frost races out from the caster's body. Anyone
 * the expanding circle sweeps over is frostbitten - the powder-snow
 * freeze meter pinned full (vignette, shiver, frozen hearts), heavy
 * slowness, and a freeze-damage tick every two seconds.
 */
public class FrostNova implements Ability {
    private final JavaPlugin plugin;

    public FrostNova(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Frost Nova"; }
    @Override public double cooldownSeconds(int tier) { return 25; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = tier >= 2 ? 11 : 8;
        int frostbiteTicks = tier >= 2 ? 120 : 80;
        int slowAmplifier = tier >= 2 ? 3 : 2; // Slowness IV / III
        Location center = caster.getLocation().clone();
        Set<UUID> swept = new HashSet<>();
        center.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.7f);

        new BukkitRunnable() {
            double r = 0.8;
            @Override public void run() {
                int points = Math.max(24, (int) (r * 9));
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = r * Math.cos(angle), z = r * Math.sin(angle);
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            center.clone().add(x, 0.3, z), 2, 0.08, 0.2, 0.08, 0.01);
                    if (i % 3 == 0) {
                        center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL,
                                center.clone().add(x, 1.1, z), 1, 0.1, 0.35, 0.1, 0.01);
                    }
                }
                for (LivingEntity target : center.getNearbyLivingEntities(r, 2.5)) {
                    double dx = target.getLocation().getX() - center.getX();
                    double dz = target.getLocation().getZ() - center.getZ();
                    if (dx * dx + dz * dz > r * r) continue; // the ring hasn't reached them
                    if (target.equals(caster) || !swept.add(target.getUniqueId())) continue;
                    frostbite(caster, target, frostbiteTicks, slowAmplifier);
                }
                r += 0.55;
                if (r > radius) cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
        return true;
    }

    private void frostbite(Player caster, LivingEntity target, int ticks, int slowAmplifier) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, slowAmplifier));
        target.getWorld().playSound(target.getLocation(),
                Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
        DamageSource frost = DamageSource.builder(DamageType.FREEZE)
                .withCausingEntity(caster).withDirectEntity(caster).build();
        new BukkitRunnable() {
            int lived = 0;
            @Override public void run() {
                if (!target.isValid() || target.isDead() || lived >= ticks) { cancel(); return; }
                lived++;
                // pin the freeze meter full each tick; once this stops the
                // vanilla 2/tick decay fades the vignette out on its own
                target.setFreezeTicks(target.getMaxFreezeTicks());
                if (lived % 5 == 0) {
                    target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            target.getLocation().add(0, 1, 0), 3, 0.25, 0.5, 0.25, 0.01);
                }
                if (lived % 40 == 0) target.damage(1.0, frost);
            }
        }.runTaskTimer(plugin, 1, 1);
    }
}
