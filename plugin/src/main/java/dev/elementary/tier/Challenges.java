package dev.elementary.tier;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.shard.Shards;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

/** Route 1 to tier 2: the per-element challenges. */
public class Challenges implements Listener {
    public static final Map<Element, Double> TARGETS = Map.of(
            Element.EARTH, 200.0,   // Unmoved: absorb damage without dying
            Element.WATER, 1000.0,  // Deep Current: swim submerged
            Element.FIRE, 50.0,     // Kindling: kill burning entities
            Element.AIR, 20.0,      // Untouched Sky: continuous airtime
            Element.AQUA, 100.0);   // Hailstorm: land Orbital Ice pellets

    private final ElementaryPlugin plugin;
    private final Map<UUID, Boolean> wasUnderwater = new HashMap<>();
    private final Map<UUID, Integer> airborneSeconds = new HashMap<>();

    public Challenges(ElementaryPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 20, 20);
    }

    public static void pelletHit(ElementaryPlugin plugin, Player shooter) {
        Challenges self = plugin.challenges();
        if (self != null) self.add(shooter, Element.AQUA, 1);
    }

    private void add(Player player, Element expected, double amount) {
        PlayerData data = plugin.shards().dataFor(player);
        if (data.element != expected || data.tier >= 2) return;
        double target = TARGETS.get(expected);
        data.challengeProgress = Math.min(target, data.challengeProgress + amount);
        int percent = (int) (100 * data.challengeProgress / target);
        if (percent >= 100) {
            plugin.shards().promote(player);
            return;
        }
        for (int milestone : new int[]{75, 50, 25}) {
            if (percent >= milestone && data.challengeMilestone < milestone) {
                data.challengeMilestone = milestone;
                player.sendMessage(Component.text(
                        "Challenge progress: " + milestone + "%", NamedTextColor.GRAY));
                break;
            }
        }
    }

    private void reset(Player player, Element expected) {
        PlayerData data = plugin.shards().dataFor(player);
        if (data.element != expected || data.tier >= 2) return;
        data.challengeProgress = 0;
        data.challengeMilestone = 0;
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!Shards.isShard(player.getInventory().getItemInOffHand())) continue;
            PlayerData data = plugin.shards().dataFor(player);
            if (data.tier >= 2) continue;
            if (data.element == Element.WATER) {
                boolean under = player.isUnderWater();
                Boolean was = wasUnderwater.put(player.getUniqueId(), under);
                if (was != null && was && !under) {
                    reset(player, Element.WATER); // broke the surface
                }
            } else if (data.element == Element.AIR) {
                boolean airborne = !player.isOnGround() && !player.isFlying()
                        && !player.isInWater() && !player.isClimbing();
                if (airborne) {
                    int seconds = airborneSeconds.merge(player.getUniqueId(), 1, Integer::sum);
                    if (seconds > data.challengeProgress) {
                        add(player, Element.AIR, seconds - data.challengeProgress);
                    }
                } else {
                    airborneSeconds.remove(player.getUniqueId());
                    // the timer resets, banked progress does not roll back
                }
            }
        }
    }

    /** Water distance is tracked from movement while fully submerged. */
    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isUnderWater()) return;
        if (!Shards.isShard(player.getInventory().getItemInOffHand())) return;
        double distance = event.getFrom().toVector().setY(0)
                .distance(event.getTo().toVector().setY(0));
        if (distance > 0.001 && distance < 1) {
            add(player, Element.WATER, distance);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAbsorb(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!Shards.isShard(player.getInventory().getItemInOffHand())) return;
        add(player, Element.EARTH, event.getFinalDamage());
    }

    @EventHandler
    public void onEarthDeath(PlayerDeathEvent event) {
        reset(event.getPlayer(), Element.EARTH);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || victim.getFireTicks() <= 0) return;
        add(killer, Element.FIRE, 1);
    }
}
