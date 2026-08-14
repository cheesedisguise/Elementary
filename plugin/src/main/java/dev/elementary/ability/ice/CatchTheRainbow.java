package dev.elementary.ability.ice;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.shard.Shards;
import dev.elementary.util.Fx;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Catch The Rainbow: while rain falls on aqudr he can double jump —
 * one extra mid-air jump per airborne stretch (two in thunderstorms at
 * tier 2), with a rainbow burst under his feet.
 */
public class CatchTheRainbow implements Listener {
    private final ElementaryPlugin plugin;
    private final Map<UUID, Integer> used = new HashMap<>();

    public CatchTheRainbow(ElementaryPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 10, 2);
    }

    private boolean managed(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL
                || player.getGameMode() == GameMode.ADVENTURE;
    }

    private int charges(Player player, PlayerData data) {
        return data.tier >= 2 && player.getWorld().isThundering() ? 2 : 1;
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!managed(player)) continue;
            if (!Shards.isShard(player.getInventory().getItemInOffHand())) continue;
            PlayerData data = plugin.shards().dataFor(player);
            if (data.element != Element.ICE) {
                continue;
            }
            if (player.isOnGround()) {
                used.remove(player.getUniqueId());
                if (player.getAllowFlight()) player.setAllowFlight(false);
                continue;
            }
            boolean eligible = player.isInRain()
                    && used.getOrDefault(player.getUniqueId(), 0) < charges(player, data);
            if (player.getAllowFlight() != eligible) {
                player.setAllowFlight(eligible);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!managed(player)) return;
        if (!Shards.isShard(player.getInventory().getItemInOffHand())) return;
        PlayerData data = plugin.shards().dataFor(player);
        if (data.element != Element.ICE) return;
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        if (!player.isInRain()) return;
        used.merge(player.getUniqueId(), 1, Integer::sum);
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() > 0.01) dir.normalize().multiply(0.35);
        player.setVelocity(dir.setY(0.9));
        player.setFallDistance(0);
        Fx.rainbowArc(player.getLocation());
        player.getWorld().playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_BREEZE_JUMP, 0.8f, 1.4f);
    }
}
