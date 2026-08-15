package dev.elementary.ability.light;

import dev.elementary.ability.Ability;
import dev.elementary.util.Targets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** Eight seconds of searing radiance - and a golden harvest: mobs the
 *  caster kills during the flare drop double loot. */
public class SolarFlare implements Ability, Listener {
    private final Map<UUID, Integer> flareUntil = new HashMap<>();

    private final JavaPlugin plugin;

    public SolarFlare(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Solar Flare"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        flareUntil.put(caster.getUniqueId(), Bukkit.getCurrentTick() + 20 * 8);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 8, 1));
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 2;
                if (ticks > 20 * 8 || !caster.isOnline()) { cancel(); return; }
                double pulse = (ticks % 20) / 20.0 * 7;
                for (int i = 0; i < 26; i++) {
                    double angle = 2 * Math.PI * i / 26;
                    caster.getWorld().spawnParticle(Particle.DUST,
                            caster.getLocation().clone().add(
                                    pulse * Math.cos(angle), 0.4, pulse * Math.sin(angle)),
                            1, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFE94A), 1.1f));
                }
                caster.getWorld().spawnParticle(Particle.END_ROD,
                        caster.getLocation().add(0, 1.4, 0), 2, 0.4, 0.5, 0.4, 0.02);
                if (ticks % 20 == 0) {
                    for (LivingEntity target : caster.getLocation()
                            .getNearbyLivingEntities(7)) {
                        if (!Targets.hostile(caster, target)) continue;
                        target.damage(2, caster);
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.GLOWING, 200, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 1.4f);
        return true;
    }

    /** The golden hour: kills during the flare shake out twice the loot. */
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        Integer until = flareUntil.get(killer.getUniqueId());
        if (until == null || Bukkit.getCurrentTick() >= until) return;
        for (ItemStack drop : new java.util.ArrayList<>(event.getDrops())) {
            event.getDrops().add(drop.clone());
        }
        event.getEntity().getWorld().spawnParticle(Particle.WAX_OFF,
                event.getEntity().getLocation().add(0, 0.8, 0), 14, 0.35, 0.4, 0.35, 0.2);
    }
}
