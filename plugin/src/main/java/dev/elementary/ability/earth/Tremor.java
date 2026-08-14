package dev.elementary.ability.earth;

import dev.elementary.ability.Ability;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Tremor implements Ability {
    private final JavaPlugin plugin;

    public Tremor(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Tremor"; }
    @Override public double cooldownSeconds(int tier) { return 25; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = tier >= 2 ? 9 : 6;
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (target.equals(caster)) continue;
            target.damage(4, caster);
            target.setVelocity(target.getVelocity().clone().setY(0.9));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
            if (tier >= 2) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 9));
            }
            caster.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 20,
                    0.3, 0.1, 0.3, Material.DIRT.createBlockData());
        }
        // expanding ground ring of stone crack racing to the edge
        new BukkitRunnable() {
            double r = 1;
            @Override public void run() {
                for (int i = 0; i < 28; i++) {
                    double angle = 2 * Math.PI * i / 28;
                    caster.getWorld().spawnParticle(Particle.BLOCK,
                            caster.getLocation().clone().add(r * Math.cos(angle), 0.15,
                                    r * Math.sin(angle)),
                            3, 0.05, 0.02, 0.05, Material.STONE.createBlockData());
                }
                r += radius / 6.0;
                if (r > radius) cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.BLOCK_DEEPSLATE_BREAK, 1f, 0.6f);
        return true;
    }
}
