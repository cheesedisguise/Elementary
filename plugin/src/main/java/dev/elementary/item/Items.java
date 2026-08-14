package dev.elementary.item;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class Items {
    private static NamespacedKey UPGRADER_KEY;
    private static NamespacedKey TRADER_KEY;

    private Items() {}

    public static void init(JavaPlugin plugin) {
        UPGRADER_KEY = new NamespacedKey(plugin, "upgrader");
        TRADER_KEY = new NamespacedKey(plugin, "shard_trader");
        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "shard_trader_recipe"), trader(plugin));
        recipe.shape("AGA", "GEG", "AGA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('E', Material.ENDER_EYE);
        plugin.getServer().addRecipe(recipe);
    }

    public static ItemStack upgrader(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Upgrader", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("A fallen player's tier, made physical.",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Right-click to ascend to Tier 2.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(UPGRADER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack trader(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Shard Trader", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Right-click to reroll into a different element.",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Wipes Tier 2 and challenge progress.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(TRADER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isUpgrader(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta()
                .getPersistentDataContainer().has(UPGRADER_KEY, PersistentDataType.BYTE);
    }

    public static boolean isTrader(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta()
                .getPersistentDataContainer().has(TRADER_KEY, PersistentDataType.BYTE);
    }
}
