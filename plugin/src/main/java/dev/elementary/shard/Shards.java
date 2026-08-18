package dev.elementary.shard;

import dev.elementary.element.Element;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Factory and inspector for shard items. */
public final class Shards {
    public static NamespacedKey ELEMENT_KEY;
    public static NamespacedKey OWNER_KEY;
    public static NamespacedKey TIER_KEY;

    private Shards() {}

    public static void init(JavaPlugin plugin) {
        ELEMENT_KEY = new NamespacedKey(plugin, "element");
        OWNER_KEY = new NamespacedKey(plugin, "owner");
        TIER_KEY = new NamespacedKey(plugin, "tier");
    }

    public static ItemStack create(Element element, UUID owner, int tier) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(element.shardName(), element.color())
                .decoration(TextDecoration.ITALIC, false));
        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        cmd.setFloats(List.of((float) element.cmd(tier)));
        meta.setCustomModelDataComponent(cmd);
        // vanish on death - the respawn re-issue is the only way back,
        // so a death can never duplicate a shard
        meta.addEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE, 1, true);
        meta.setEnchantmentGlintOverride(false); // tier 2's glow texture stays the only shine
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ELEMENT_KEY, PersistentDataType.STRING, element.name());
        pdc.set(OWNER_KEY, PersistentDataType.STRING, owner.toString());
        pdc.set(TIER_KEY, PersistentDataType.INTEGER, tier);
        item.setItemMeta(meta);
        if (element == Element.LIGHT) {
            // Sunspear charges by HOLDING right-click. A plain item
            // never reports the button being held, so the Light shard
            // is an unfinishable consumable: silent, invisible, twenty
            // hours to "eat" - but the server now sees press and
            // release exactly.
            item.setData(io.papermc.paper.datacomponent.DataComponentTypes.CONSUMABLE,
                    io.papermc.paper.datacomponent.item.Consumable.consumable()
                            .consumeSeconds(72000f)
                            .animation(io.papermc.paper.datacomponent.item.consumable
                                    .ItemUseAnimation.NONE)
                            .sound(net.kyori.adventure.key.Key.key(
                                    "minecraft", "intentionally_empty"))
                            .hasConsumeParticles(false));
        }
        return item;
    }

    public static boolean isShard(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(ELEMENT_KEY, PersistentDataType.STRING);
    }

    public static Element element(ItemStack item) {
        String name = item.getItemMeta().getPersistentDataContainer()
                .get(ELEMENT_KEY, PersistentDataType.STRING);
        return name == null ? null : Element.valueOf(name);
    }

    public static UUID owner(ItemStack item) {
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(OWNER_KEY, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    public static int tier(ItemStack item) {
        Integer t = item.getItemMeta().getPersistentDataContainer()
                .get(TIER_KEY, PersistentDataType.INTEGER);
        return t == null ? 1 : t;
    }
}
