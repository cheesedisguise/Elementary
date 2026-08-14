package dev.elementary.command;

import dev.elementary.ElementaryPlugin;
import dev.elementary.element.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /broker <player> <shard> - apply the exchange to anyone, free. */
public class BrokerCommand implements TabExecutor {
    private final ElementaryPlugin plugin;

    public BrokerCommand(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /broker <player> <shard>",
                    NamedTextColor.RED));
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0],
                    NamedTextColor.RED));
            return true;
        }
        Element element;
        try {
            element = Element.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown shard: " + args[1]
                    + " (earth, water, fire, air, ice, shadow, light, lightning)",
                    NamedTextColor.RED));
            return true;
        }
        Element bound = plugin.shards().boundElement(target.getUniqueId());
        if (bound != null && bound != element) {
            sender.sendMessage(Component.text(target.getName() + " is bound to "
                    + bound.displayName() + " in the config - edit bound-players "
                    + "and /elementary reload first.", NamedTextColor.RED));
            return true;
        }
        plugin.shards().applyElement(target, element);
        sender.sendMessage(Component.text("Set ", NamedTextColor.GRAY)
                .append(target.displayName())
                .append(Component.text(" to the ", NamedTextColor.GRAY))
                .append(Component.text(element.shardName(), element.color()))
                .append(Component.text(".", NamedTextColor.GRAY)));
        target.sendMessage(Component.text("The exchange is made: ", NamedTextColor.GRAY)
                .append(Component.text(element.shardName(), element.color())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label,
                                      String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT)
                        .startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            for (Element element : Element.values()) {
                String name = element.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(name);
                }
            }
        }
        return out;
    }
}
