package dev.elementary.command;

import dev.elementary.ElementaryPlugin;
import dev.elementary.cooldown.Cooldowns;
import dev.elementary.data.PlayerData;
import dev.elementary.tier.Challenges;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoCommand implements CommandExecutor {
    private final ElementaryPlugin plugin;

    public InfoCommand(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        PlayerData data = plugin.shards().dataFor(player);
        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            data.abilityMessages = !data.abilityMessages;
            plugin.store().save();
            player.sendMessage(Component.text("Ability messages "
                    + (data.abilityMessages ? "on" : "off"), NamedTextColor.GRAY));
            return true;
        }
        player.sendMessage(Component.text("\u25c6 ", data.element.color())
                .append(Component.text(data.element.shardName(), data.element.color()))
                .append(Component.text("  Tier " + data.tier, NamedTextColor.GRAY)));
        double target = Challenges.TARGETS.getOrDefault(data.element, 100.0);
        if (data.tier < 2) {
            player.sendMessage(Component.text(String.format(
                    "Challenge: %.0f / %.0f (%d%%)", data.challengeProgress, target,
                    (int) (100 * data.challengeProgress / target)), NamedTextColor.GRAY));
        }
        Map<String, Cooldowns.Entry> cds = plugin.cooldowns().all(player.getUniqueId());
        for (Map.Entry<String, Cooldowns.Entry> e : cds.entrySet()) {
            long left = e.getValue().endMs() - System.currentTimeMillis();
            if (left <= 0) continue;
            player.sendMessage(Component.text(String.format("  %s \u2014 %.1fs",
                    e.getKey(), left / 1000.0), NamedTextColor.DARK_GRAY));
        }
        return true;
    }
}
