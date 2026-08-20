package dev.elementary.msg;

import dev.elementary.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Ability messages are client-sided: only the caster ever sees them. */
public final class Msg {
    private Msg() {}

    public static void used(Player caster, PlayerData data, String ability) {
        if (!data.abilityMessages) return;
        caster.sendMessage(Component.text("You used " + ability + "!", data.element.color()));
    }

    public static void ultimate(Player caster, PlayerData data, String ability) {
        if (data.abilityMessages) {
            caster.sendMessage(Component.text("\u26a1 ", NamedTextColor.YELLOW)
                    .append(Component.text("You unleashed " + ability.toUpperCase() + "!",
                            data.element.color()).decorate(TextDecoration.BOLD)));
        }
        caster.playSound(caster.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.35f, 1.6f);
    }

    /** Failures go to CHAT, never the action bar - the action bar is
     *  the ability HUD's, and a message there stomps the icons. */
    public static void fail(Player caster, String reason) {
        caster.sendMessage(Component.text(reason, NamedTextColor.RED));
    }

    public static void cooldown(Player caster, long remainingMs) {
        fail(caster, String.format("On cooldown \u2014 %.1fs", remainingMs / 1000.0));
    }
}
