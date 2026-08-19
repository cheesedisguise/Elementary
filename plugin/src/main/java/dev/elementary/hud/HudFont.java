package dev.elementary.hud;

import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

/**
 * The elementary:hud font, kept in sync with gen_textures.py: ability
 * icons on U+E000+, tall digits on ASCII 0-9, and space glyphs for
 * pixel positioning (negative E100..E105, positive E108..E10C).
 */
public final class HudFont {
    public static final Key FONT = Key.key("elementary", "hud");
    public static final int ICON_ADVANCE = 17;
    public static final int DIGIT_ADVANCE = 6;

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("Fissure", "\uE000"),
            Map.entry("Boulder", "\uE001"),
            Map.entry("Cataclysm", "\uE002"),
            Map.entry("Tide Pull", "\uE003"),
            Map.entry("Healing Spring", "\uE004"),
            Map.entry("Maelstrom", "\uE005"),
            Map.entry("Fireball", "\uE006"),
            Map.entry("Pyre", "\uE007"),
            Map.entry("Meteor Shower", "\uE008"),
            Map.entry("Updraft", "\uE009"),
            Map.entry("Gale", "\uE00A"),
            Map.entry("Tempest", "\uE00B"),
            Map.entry("Frozen Over", "\uE00C"),
            Map.entry("Orbital Ice", "\uE00D"),
            Map.entry("Sub-Zero", "\uE00E"),
            Map.entry("Neural Overload", "\uE00F"),
            Map.entry("Sunspear", "\uE010"),
            Map.entry("Supernova", "\uE011"),
            Map.entry("Shade Daggers", "\uE012"),
            Map.entry("Shadestep", "\uE013"),
            Map.entry("Hunt", "\uE014"),
            Map.entry("Volt Dash", "\uE015"),
            Map.entry("Emotion Wave", "\uE016"),
            Map.entry("Powerplant", "\uE017"),
            Map.entry("Radiance", "\uE018"),
            Map.entry("Fear", "\uE019"),
            Map.entry("Luminosity", "\uE01A"),
            Map.entry("Harmony", "\uE01B"),
            Map.entry("Concussion", "\uE01C"),
            Map.entry("Absolute Radiance", "\uE01D"));

    private static final int[] NEGATIVE = {32, 16, 8, 4, 2, 1};
    private static final char[] NEGATIVE_CHARS =
            {'\uE105', '\uE104', '\uE103', '\uE102', '\uE101', '\uE100'};
    private static final int[] POSITIVE = {16, 8, 4, 2, 1};
    private static final char[] POSITIVE_CHARS =
            {'\uE10C', '\uE10B', '\uE10A', '\uE109', '\uE108'};

    private HudFont() {}

    public static String icon(String abilityName) {
        return ICONS.get(abilityName);
    }

    /** A cursor shift of any pixel amount, composed from space glyphs. */
    public static String offset(int pixels) {
        StringBuilder sb = new StringBuilder();
        if (pixels < 0) {
            int rest = -pixels;
            for (int i = 0; i < NEGATIVE.length; i++) {
                while (rest >= NEGATIVE[i]) {
                    sb.append(NEGATIVE_CHARS[i]);
                    rest -= NEGATIVE[i];
                }
            }
        } else {
            int rest = pixels;
            for (int i = 0; i < POSITIVE.length; i++) {
                while (rest >= POSITIVE[i]) {
                    sb.append(POSITIVE_CHARS[i]);
                    rest -= POSITIVE[i];
                }
            }
        }
        return sb.toString();
    }

    public static TextComponent text(String glyphs, TextColor color) {
        return Component.text(glyphs).font(FONT).color(color);
    }
}
