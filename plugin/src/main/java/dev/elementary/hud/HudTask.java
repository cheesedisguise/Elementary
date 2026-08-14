package dev.elementary.hud;

import dev.elementary.ElementaryPlugin;
import dev.elementary.cooldown.Cooldowns;
import dev.elementary.data.PlayerData;
import dev.elementary.shard.Shards;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Option A HUD: one boss bar, visible only while the shard is in the
 * offhand. The bar fills as the longest-running cooldown recharges -
 * red while recharging, green when everything is ready.
 */
public class HudTask extends BukkitRunnable {
    private final ElementaryPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public HudTask(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean holding = Shards.isShard(player.getInventory().getItemInOffHand());
            BossBar bar = bars.get(player.getUniqueId());
            if (!holding) {
                if (bar != null) {
                    player.hideBossBar(bar);
                    bars.remove(player.getUniqueId());
                }
                continue;
            }
            PlayerData data = plugin.shards().dataFor(player);
            if (bar == null) {
                bar = BossBar.bossBar(Component.empty(), 1f,
                        BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
                bars.put(player.getUniqueId(), bar);
                player.showBossBar(bar);
            }
            Cooldowns.Entry recharging = plugin.cooldowns().mostRecent(player.getUniqueId());
            String title = "\u25c6 " + data.element.displayName()
                    + (data.tier >= 2 ? " II" : "");
            if (recharging == null) {
                bar.name(Component.text(title + " \u2014 abilities ready", data.element.color()));
                bar.progress(1f);
                bar.color(BossBar.Color.GREEN);
            } else {
                long left = recharging.endMs() - System.currentTimeMillis();
                float progress = 1f - (float) left / recharging.totalMs();
                bar.name(Component.text(String.format("%s \u2014 %.1fs", title, left / 1000.0),
                        data.element.color()));
                bar.progress(Math.max(0f, Math.min(1f, progress)));
                bar.color(BossBar.Color.RED);
            }
        }
    }
}
