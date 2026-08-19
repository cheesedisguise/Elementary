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
    private final NamespacedKey rootedKey;

    public PassiveTask(ElementaryPlugin plugin) {
        this.plugin = plugin;
        this.healthKey = new NamespacedKey(plugin, "earth_health");
        this.rootedKey = new NamespacedKey(plugin, "earth_rooted");
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // passives are always on - the shard grants them by being yours
            PlayerData data = plugin.shards().dataFor(player);
            boolean earth = data.element == dev.elementary.element.Element.EARTH;
            applyModifier(player, Attribute.MAX_HEALTH, healthKey,
                    earth ? (data.tier >= 2 ? 6.0 : 4.0) : 0.0);
            // Unshakeable: rooted like stone while standing on it
            applyModifier(player, Attribute.KNOCKBACK_RESISTANCE, rootedKey,
                    earth && onStone(player) ? (data.tier >= 2 ? 0.9 : 0.6) : 0.0);
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

    private void applyModifier(Player player, Attribute attribute, NamespacedKey key,
                               double amount) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr == null) return;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (mod.getKey().equals(key)) {
                if (mod.getAmount() == amount) return;
                attr.removeModifier(mod);
            }
        }
        if (amount > 0) {
            attr.addModifier(new AttributeModifier(key, amount,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private boolean onStone(Player player) {
        Material type = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        String name = type.name();
        return org.bukkit.Tag.BASE_STONE_OVERWORLD.isTagged(type)
                || org.bukkit.Tag.DIRT.isTagged(type)
                || name.contains("DEEPSLATE") || name.contains("STONE");
    }

    private final java.util.Map<java.util.UUID, Long> lastHurt = new java.util.HashMap<>();

    /** CombatListener reports every hit a player takes. */
    public void notePain(Player player) {
        lastHurt.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void earth(Player player, PlayerData data) {
        // stone plates: absorption armour that regrows after 5s unhurt
        Long hurt = lastHurt.get(player.getUniqueId());
        if (hurt == null || System.currentTimeMillis() - hurt > 5000) {
            give(player, PotionEffectType.ABSORPTION, data.tier >= 2 ? 1 : 0);
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
        // Run For Your Life: the fear-every-4th-hit and the backstab
        // bonus both live in CombatListener; nothing timed here
    }

    private final java.util.Map<java.util.UUID, org.bukkit.util.Vector> lastPos =
            new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Double> meter = new java.util.HashMap<>();

    /** Over-Charged's Momentum meter, in smoothed blocks-per-second. */
    public double momentum(Player player) {
        return meter.getOrDefault(player.getUniqueId(), 0.0);
    }

    /**
     * Over-Charged: Momentum is a smoothed measure of recent speed -
     * each second the meter keeps half of itself and adds the blocks
     * just travelled. Plain sprinting settles around 11; you need
     * sprint-jumping or the meter's own Speed gifts to cross the 12.5
     * zap threshold. Powerplant raises the cap to 20 and charges 50%
     * faster.
     */
    private void lightning(Player player, PlayerData data) {
        org.bukkit.util.Vector now = player.getLocation().toVector().setY(0);
        org.bukkit.util.Vector previous = lastPos.put(player.getUniqueId(), now);
        double dist = previous == null ? 0 : Math.min(25, previous.distance(now));
        boolean plant = dev.elementary.ability.lightning.Powerplant.active(player);
        double cap = plant ? 20 : 15;
        double charge = Math.min(cap,
                meter.getOrDefault(player.getUniqueId(), 0.0) * 0.5
                        + dist * (plant ? 1.5 : 1.0));
        meter.put(player.getUniqueId(), charge);
        int amp = charge >= 13 ? 2 : charge >= 9 ? 1 : charge >= 5 ? 0 : -1;
        if (amp >= 0) give(player, PotionEffectType.SPEED, amp);
        if (charge >= 12.5) {
            // fully overcharged: the body arcs - zaps are armed
            player.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                    player.getLocation().add(0, 0.9, 0), 4, 0.25, 0.4, 0.25, 0.02);
        } else if (charge >= 9) {
            player.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                    player.getLocation().add(0, 0.2, 0), 2, 0.2, 0.05, 0.2, 0.01);
        }
    }

    private void light(Player player, PlayerData data) {
        // Radiance carries the whole Light passive (status/Radiance)
    }
}
