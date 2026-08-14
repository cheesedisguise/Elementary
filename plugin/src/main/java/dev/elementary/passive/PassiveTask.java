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
            // passives are always on - the shard grants them by being yours
            PlayerData data = plugin.shards().dataFor(player);
            applyEarthHealth(player, data.element == dev.elementary.element.Element.EARTH
                    ? (data.tier >= 2 ? 6.0 : 4.0) : 0.0);
            switch (data.element) {
                case EARTH -> earth(player, data);
                case WATER -> water(player, data);
                case FIRE -> fire(player, data);
                case AIR -> air(player, data);
                case ICE -> ice(player, data);
                case SHADOW -> shadow(player, data);
                case LIGHT -> light(player, data);
                case LIGHTNING -> lightning(player, data);
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

    private void ice(Player player, PlayerData data) {
        // movement passive lives in CatchTheRainbow; nothing timed here
    }

    private void shadow(Player player, PlayerData data) {
        // Umbra: quick in the dark (backstab lives in CombatListener)
        if (player.getLocation().getBlock().getLightLevel() <= 7) {
            give(player, PotionEffectType.SPEED, 0);
        }
    }

    private final java.util.Map<java.util.UUID, org.bukkit.util.Vector> lastPos =
            new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Long> lastMoved = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> momentum = new java.util.HashMap<>();

    /** Momentum: moving builds Speed, standing still for 2s drains it. */
    private void lightning(Player player, PlayerData data) {
        org.bukkit.util.Vector now = player.getLocation().toVector().setY(0);
        org.bukkit.util.Vector previous = lastPos.put(player.getUniqueId(), now);
        long ms = System.currentTimeMillis();
        boolean moved = previous != null && previous.distanceSquared(now) > 0.06;
        if (moved) {
            lastMoved.put(player.getUniqueId(), ms);
            int stacks = Math.min(3, momentum.merge(player.getUniqueId(), 1, Integer::sum));
            momentum.put(player.getUniqueId(), stacks);
            give(player, PotionEffectType.SPEED, stacks - 1);
            if (stacks >= 2) {
                player.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                        player.getLocation().add(0, 0.2, 0), 2, 0.2, 0.05, 0.2, 0.01);
            }
        } else if (ms - lastMoved.getOrDefault(player.getUniqueId(), 0L) > 2000) {
            momentum.remove(player.getUniqueId());
        } else {
            int stacks = momentum.getOrDefault(player.getUniqueId(), 0);
            if (stacks > 0) give(player, PotionEffectType.SPEED, stacks - 1);
        }
    }

    private void light(Player player, PlayerData data) {
        // Lumen: permanent night vision, regeneration in sunlight
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                400, 0, true, false, false));
        boolean day = player.getWorld().isDayTime();
        boolean openSky = player.getLocation().getBlock().getLightFromSky() >= 15;
        if (day && (openSky || data.tier >= 2)) {
            give(player, PotionEffectType.REGENERATION, 0);
        }
    }
}
