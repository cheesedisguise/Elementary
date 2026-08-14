package dev.elementary.hud;

import dev.elementary.ElementaryPlugin;
import dev.elementary.ability.Ability;
import dev.elementary.ability.AbilityManager;
import dev.elementary.cooldown.Cooldowns;
import dev.elementary.data.PlayerData;
import dev.elementary.shard.Shards;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The ability HUD. Default style "font": icons drawn above the hotbar
 * through the elementary:hud font, each with its remaining cooldown
 * floating on top. "bossbar" keeps the old single-bar HUD for servers
 * without the resource pack; "off" disables it.
 */
public class HudTask extends BukkitRunnable implements org.bukkit.event.Listener {
    private final ElementaryPlugin plugin;
    private final String style;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final java.util.Set<UUID> packLoaded = new java.util.HashSet<>();

    public HudTask(ElementaryPlugin plugin) {
        this.plugin = plugin;
        this.style = plugin.getConfig().getString("hud-style", "font");
    }

    /** Players who confirmed the server resource pack get font glyphs;
     *  everyone else falls back to the boss bar instead of tofu boxes. */
    @org.bukkit.event.EventHandler
    public void onPackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent event) {
        if (event.getStatus()
                == org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
                        .SUCCESSFULLY_LOADED) {
            packLoaded.add(event.getPlayer().getUniqueId());
        } else if (event.getStatus()
                == org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.DECLINED
                || event.getStatus()
                == org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
                        .FAILED_DOWNLOAD) {
            packLoaded.remove(event.getPlayer().getUniqueId());
        }
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        packLoaded.remove(event.getPlayer().getUniqueId());
    }

    private boolean fontFor(Player player) {
        if ("font-force".equalsIgnoreCase(style)) return true;
        return packLoaded.contains(player.getUniqueId());
    }

    @Override
    public void run() {
        if ("off".equalsIgnoreCase(style)) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean holding = Shards.isShard(player.getInventory().getItemInMainHand());
            if ("bossbar".equalsIgnoreCase(style) || !fontFor(player)) {
                bossbar(player, holding);
            } else {
                bossbar(player, false); // hide any lingering bar
                if (holding) player.sendActionBar(fontLine(player));
            }
        }
    }

    // ------------------------------------------------------------ font
    private Component fontLine(Player player) {
        PlayerData data = plugin.shards().dataFor(player);
        AbilityManager.Kit kit = plugin.abilities().kit(data.element);
        if (kit == null) return Component.empty();
        TextComponent.Builder line = Component.text();
        segment(line, player, data, kit.primary(), false);
        line.append(HudFont.text(HudFont.offset(6), NamedTextColor.WHITE));
        segment(line, player, data, kit.secondary(), false);
        line.append(HudFont.text(HudFont.offset(6), NamedTextColor.WHITE));
        segment(line, player, data, kit.ultimate(), data.tier < 2);
        return line.build();
    }

    private void segment(TextComponent.Builder line, Player player, PlayerData data,
                         Ability ability, boolean locked) {
        String icon = HudFont.icon(ability.name());
        if (icon == null) return;
        long left = plugin.cooldowns().remainingMs(player.getUniqueId(), ability.name());
        boolean cooling = left > 0;
        line.append(HudFont.text(icon,
                locked ? NamedTextColor.DARK_GRAY
                        : cooling ? NamedTextColor.GRAY : data.element.color()));
        if (!locked && cooling) {
            String digits = String.valueOf((left + 999) / 1000);
            int width = digits.length() * HudFont.DIGIT_ADVANCE;
            int back = HudFont.ICON_ADVANCE - Math.max(0,
                    (HudFont.ICON_ADVANCE - width) / 2) - width;
            line.append(HudFont.text(
                    HudFont.offset(-HudFont.ICON_ADVANCE
                            + Math.max(0, (HudFont.ICON_ADVANCE - width) / 2)),
                    NamedTextColor.WHITE));
            line.append(HudFont.text(digits, NamedTextColor.WHITE));
            line.append(HudFont.text(HudFont.offset(Math.max(0, back)),
                    NamedTextColor.WHITE));
        }
    }

    // --------------------------------------------------------- bossbar
    private void bossbar(Player player, boolean holding) {
        BossBar bar = bars.get(player.getUniqueId());
        if (!holding) {
            if (bar != null) {
                player.hideBossBar(bar);
                bars.remove(player.getUniqueId());
            }
            return;
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
