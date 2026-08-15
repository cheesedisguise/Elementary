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
    private static NamespacedKey BROKER_KEY;

    private Items() {}

    public static void init(JavaPlugin plugin) {
        UPGRADER_KEY = new NamespacedKey(plugin, "upgrader");
        TRADER_KEY = new NamespacedKey(plugin, "shard_trader");
        BROKER_KEY = new NamespacedKey(plugin, "shard_broker");
        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "shard_trader_recipe"), trader(plugin));
        recipe.shape("AGA", "GEG", "AGA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('E', Material.ENDER_EYE);
        plugin.getServer().addRecipe(recipe);

        // diamonds in the corners, gold on the sides, a barrel in the middle
        ShapedRecipe broker = new ShapedRecipe(
                new NamespacedKey(plugin, "shard_broker_recipe"), broker(plugin));
        broker.shape("DGD", "GBG", "DGD");
        broker.setIngredient('D', Material.DIAMOND);
        broker.setIngredient('G', Material.GOLD_INGOT);
        broker.setIngredient('B', Material.BARREL);
        plugin.getServer().addRecipe(broker);
    }

    /** The enchanted barrel: pick your element instead of gambling. */
    public static ItemStack broker(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.BARREL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Shard Broker", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Right-click to choose your element.",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Your tier travels with you; challenge progress resets.",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(BROKER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBroker(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta()
                .getPersistentDataContainer().has(BROKER_KEY, PersistentDataType.BYTE);
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
                Component.text("Your tier travels with you; challenge progress resets.",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
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
