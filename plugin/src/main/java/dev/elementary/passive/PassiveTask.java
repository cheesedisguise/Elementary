package dev.elementary.passive;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.shard.Shards;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** Applies passives every second while the shard is in the offhand. */
public class PassiveTask extends BukkitRunnable {
    private static final int DURATION = 45; // ticks, refreshed every 20

    private final ElementaryPlugin plugin;
    private final NamespacedKey healthKey;

    public PassiveTask(ElementaryPlugin plugin) {
        this.plugin = plugin;
        this.healthKey = new NamespacedKey(plugin, "earth_health");
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean holding = Shards.isShard(player.getInventory().getItemInOffHand());
            PlayerData data = plugin.shards().dataFor(player);
            applyEarthHealth(player, holding && data.element == dev.elementary.element.Element.EARTH
                    ? (data.tier >= 2 ? 6.0 : 4.0) : 0.0);
            if (!holding) continue;
            switch (data.element) {
                case EARTH -> earth(player, data);
                case WATER -> water(player, data);
                case FIRE -> fire(player, data);
                case AIR -> air(player, data);
                case AQUA -> aqua(player, data);
            }
        }
    }

    private void give(Player player, PotionEffectType type, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, DURATION, amplifier, true, false, false));
    }

    private void applyEarthHealth(Player player, double bonus) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (mod.getKey().equals(healthKey)) {
                if (mod.getAmount() == bonus) return;
                attr.removeModifier(mod);
            }
        }
        if (bonus > 0) {
            attr.addModifier(new AttributeModifier(healthKey, bonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void earth(Player player, PlayerData data) {
        Block below = player.getLocation().subtract(0, 1, 0).getBlock();
        Material type = below.getType();
        String name = type.name();
        boolean stony = org.bukkit.Tag.BASE_STONE_OVERWORLD.isTagged(type)
                || org.bukkit.Tag.DIRT.isTagged(type)
                || name.contains("DEEPSLATE") || name.contains("STONE");
        if (stony) {
            give(player, PotionEffectType.HASTE, 0);
            if (data.tier >= 2) give(player, PotionEffectType.RESISTANCE, 0);
        }
    }

    private void water(Player player, PlayerData data) {
        give(player, PotionEffectType.WATER_BREATHING, 0);
        give(player, PotionEffectType.DOLPHINS_GRACE, 0);
        give(player, PotionEffectType.CONDUIT_POWER, 0);
        boolean wet = player.isInWaterOrRain();
        if (wet) give(player, PotionEffectType.REGENERATION, data.tier >= 2 ? 1 : 0);
    }

    private void fire(Player player, PlayerData data) {
        give(player, PotionEffectType.FIRE_RESISTANCE, 0);
        player.setFireTicks(0);
    }

    private void air(Player player, PlayerData data) {
        give(player, PotionEffectType.SPEED, data.tier >= 2 ? 1 : 0);
        if (player.isSneaking() && !player.isOnGround()) {
            give(player, PotionEffectType.SLOW_FALLING, 0);
        }
    }

    private void aqua(Player player, PlayerData data) {
        // movement passive lives in CatchTheRainbow; nothing timed here
    }
}
