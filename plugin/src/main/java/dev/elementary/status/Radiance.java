package dev.elementary.status;

import dev.elementary.ElementaryPlugin;
import dev.elementary.element.Element;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Radiance - the Light passive. A stack gathers every 7.5 seconds, five
 * at most, and the kit spends them like ammunition. Each stack held:
 * the bearer shines brighter (aura particles and a real, invisible
 * light block following their feet), undead near them catch fire more
 * fiercely, and from three stacks up no invisibility works against
 * them - hidden things are painted gold on the bearer's screen.
 */
public class Radiance extends BukkitRunnable {
    public static final int MAX = 5;
    private static final int TICKS_PER_STACK = 150; // 7.5 seconds

    private final ElementaryPlugin plugin;
    private final Map<UUID, Integer> stacks = new HashMap<>();
    private final Map<UUID, Integer> nextGain = new HashMap<>();
    private final Map<UUID, Location> lightBlocks = new HashMap<>();

    public Radiance(ElementaryPlugin plugin) { this.plugin = plugin; }

    public int stacks(Player player) {
        return stacks.getOrDefault(player.getUniqueId(), 0);
    }

    /** Spends exactly n stacks; false (and no change) if short. */
    public boolean spend(Player player, int n) {
        int have = stacks(player);
        if (have < n) return false;
        stacks.put(player.getUniqueId(), have - n);
        return true;
    }

    /** Spends the whole bank and reports how much it was. */
    public int spendAll(Player player) {
        Integer have = stacks.remove(player.getUniqueId());
        return have == null ? 0 : have;
    }

    @Override
    public void run() {
        int now = Bukkit.getCurrentTick();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean light = plugin.shards().dataFor(player).element == Element.LIGHT
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR;
            if (!light) {
                stacks.remove(player.getUniqueId());
                nextGain.remove(player.getUniqueId());
                douse(player.getUniqueId());
                continue;
            }
            int have = stacks.getOrDefault(player.getUniqueId(), 0);
            Integer due = nextGain.get(player.getUniqueId());
            if (due == null) {
                nextGain.put(player.getUniqueId(), now + TICKS_PER_STACK);
            } else if (now >= due) {
                nextGain.put(player.getUniqueId(), now + TICKS_PER_STACK);
                if (have < MAX) {
                    have++;
                    stacks.put(player.getUniqueId(), have);
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.9f + 0.18f * have);
                    player.getWorld().spawnParticle(Particle.END_ROD,
                            player.getLocation().add(0, 1.2, 0), 4 + 2 * have,
                            0.3, 0.5, 0.3, 0.03);
                }
            }
            aura(player, have, now);
            lightUp(player, have);
            if (now % 20 < 5) burnUndead(player, have);
            if (have >= 3 && now % 10 < 5) revealHidden(player);
        }
        // players gone offline leave their light behind - put it out
        lightBlocks.keySet().removeIf(id -> {
            if (Bukkit.getPlayer(id) != null) return false;
            restore(lightBlocks.get(id));
            return true;
        });
    }

    /** Brighter with every stack held. */
    private void aura(Player player, int have, int now) {
        if (have <= 0 || now % 10 >= 5) return;
        player.getWorld().spawnParticle(Particle.DUST,
                player.getLocation().add(0, 1.3, 0), have,
                0.35, 0.55, 0.35, 0,
                new Particle.DustOptions(Color.fromRGB(0xFFE08A), 1.0f));
        if (have >= 3) {
            player.getWorld().spawnParticle(Particle.END_ROD,
                    player.getLocation().add(0, 1.1, 0), 1, 0.3, 0.5, 0.3, 0.01);
        }
    }

    /** A real light block shadows the bearer's feet, brighter per stack. */
    private void lightUp(Player player, int have) {
        UUID id = player.getUniqueId();
        Block feet = player.getLocation().getBlock();
        Location previous = lightBlocks.get(id);
        int level = Math.min(15, 9 + have);
        if (previous != null && previous.getBlock().equals(feet)) {
            Block block = previous.getBlock();
            if (block.getType() == Material.LIGHT
                    && block.getBlockData() instanceof Levelled lit
                    && lit.getLevel() != level) {
                lit.setLevel(level);
                block.setBlockData(lit, false);
            }
            return;
        }
        douse(id);
        if (feet.getType() != Material.AIR) return;
        Levelled data = (Levelled) Material.LIGHT.createBlockData();
        data.setLevel(level);
        feet.setBlockData(data, false);
        lightBlocks.put(id, feet.getLocation());
    }

    private void douse(UUID id) {
        restore(lightBlocks.remove(id));
    }

    private void restore(Location at) {
        if (at == null) return;
        Block block = at.getBlock();
        if (block.getType() == Material.LIGHT) block.setType(Material.AIR, false);
    }

    /** The undead cannot stand near the light - harder per stack. */
    private void burnUndead(Player player, int have) {
        double radius = 2 + have;
        for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
            if (target instanceof Player || target.equals(player)) continue;
            if (!Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(target.getType())) continue;
            target.setFireTicks(Math.max(target.getFireTicks(), 30 + 25 * have));
        }
    }

    /** Three stacks up: invisibility means nothing to the bearer. */
    private void revealHidden(Player player) {
        for (LivingEntity target : player.getLocation().getNearbyLivingEntities(24)) {
            if (target.equals(player)) continue;
            if (!target.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
            Location base = target.getLocation();
            for (double y = 0.2; y <= 1.8; y += 0.53) {
                player.spawnParticle(Particle.DUST, base.clone().add(0, y, 0), 1,
                        0.12, 0.1, 0.12, 0,
                        new Particle.DustOptions(Color.fromRGB(0xFFE94A), 1.3f));
            }
        }
    }

    /** Shutdown: no orphaned light blocks in anyone's world. */
    public void shutdown() {
        for (Location at : lightBlocks.values()) restore(at);
        lightBlocks.clear();
    }
}
