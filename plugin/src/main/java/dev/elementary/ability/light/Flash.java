package dev.elementary.ability.light;

import dev.elementary.ability.Ability;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** A blinding burst: everyone near is blinded and lit up. */
public class Flash implements Ability {
    private final JavaPlugin plugin;

    public Flash(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Flash"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = tier >= 2 ? 9 : 6;
        caster.getWorld().spawnParticle(Particle.FLASH,
                caster.getLocation().add(0, 1, 0), 1);
        caster.getWorld().spawnParticle(Particle.END_ROD,
                caster.getLocation().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0.25);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.8f);
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (target.equals(caster)) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
        }
        return true;
    }
}
