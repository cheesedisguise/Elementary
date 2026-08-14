package dev.elementary.ability.water;

import dev.elementary.ability.Ability;
import dev.elementary.util.TrueDamage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** A storm cloud follows the caster; every melee hit calls lightning. */
public class Thunderstorm implements Ability, Listener {
    private record Storm(long endMs, int tier) {}

    private final JavaPlugin plugin;
    private final Map<UUID, Storm> storms = new HashMap<>();

    public Thunderstorm(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Thunderstorm"; }
    @Override public double cooldownSeconds(int tier) { return 40; }

    @Override
    public boolean cast(Player caster, int tier) {
        int seconds = tier >= 2 ? 15 : 10;
        storms.put(caster.getUniqueId(),
                new Storm(System.currentTimeMillis() + seconds * 1000L, tier));
        new BukkitRunnable() {
            @Override public void run() {
                Storm storm = storms.get(caster.getUniqueId());
                if (storm == null || System.currentTimeMillis() > storm.endMs()
                        || !caster.isOnline()) {
                    storms.remove(caster.getUniqueId());
                    cancel();
                    return;
                }
                org.bukkit.Location cloud = caster.getLocation().add(0, 3.2, 0);
                caster.getWorld().spawnParticle(Particle.CLOUD, cloud, 8, 0.9, 0.15, 0.9, 0.005);
                caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, cloud, 2,
                        0.7, 0.1, 0.7, 0.02);
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        Storm storm = storms.get(attacker.getUniqueId());
        if (storm == null || System.currentTimeMillis() > storm.endMs()) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        victim.getWorld().strikeLightningEffect(victim.getLocation());
        double bonus = storm.tier() >= 2 ? 3 : 2;
        plugin.getServer().getScheduler().runTask(plugin,
                () -> TrueDamage.apply(victim, bonus, attacker));
    }
}
