package dev.elementary.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * A temporary re-skin of the ground: the topmost solid block of each
 * column inside a circle becomes a themed material, spreading outward
 * from the centre; when the effect ends the border recedes back in,
 * restoring every original block exactly. Used by Pyre and Frozen Over.
 */
public class GroundCover {
    public interface Palette { Material pick(ThreadLocalRandom rng); }

    private record Column(double dist, Block block, BlockData original, Material skin) {}

    /** Live covers, so a plugin disable can put every block back. */
    private static final Set<GroundCover> ACTIVE = new HashSet<>();

    private final JavaPlugin plugin;
    private final List<Column> columns = new ArrayList<>();
    private final Set<Block> covered = new HashSet<>();
    private int frontier = 0; // first column not yet converted
    private BukkitTask task;

    private GroundCover(JavaPlugin plugin) { this.plugin = plugin; }

    public static GroundCover spread(JavaPlugin plugin, Location center, double radius,
                                     double blocksPerTick, Palette palette) {
        GroundCover cover = new GroundCover(plugin);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                Block top = topSolid(center, dx, dz);
                if (top == null || !convertible(top)) continue;
                cover.columns.add(new Column(dist, top, top.getBlockData(),
                        palette.pick(rng)));
            }
        }
        cover.columns.sort(Comparator.comparingDouble(Column::dist));
        ACTIVE.add(cover);
        cover.task = new BukkitRunnable() {
            double reach = 0;
            @Override public void run() {
                reach += blocksPerTick;
                while (cover.frontier < cover.columns.size()
                        && cover.columns.get(cover.frontier).dist() <= reach) {
                    Column column = cover.columns.get(cover.frontier++);
                    column.block().setType(column.skin(), false);
                    cover.covered.add(column.block());
                }
                if (cover.frontier >= cover.columns.size()) cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
        return cover;
    }

    public boolean isCovered(Block block) { return covered.contains(block); }

    /** The border shrinks back to the centre, restoring as it goes. */
    public void recede(double blocksPerTick) {
        if (task != null) task.cancel();
        task = new BukkitRunnable() {
            double reach = outerEdge();
            @Override public void run() {
                reach -= blocksPerTick;
                while (frontier > 0 && columns.get(frontier - 1).dist() > reach) {
                    restore(columns.get(--frontier));
                }
                if (frontier <= 0) {
                    ACTIVE.remove(GroundCover.this);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private double outerEdge() {
        return frontier > 0 ? columns.get(frontier - 1).dist() : 0;
    }

    private void restore(Column column) {
        column.block().setBlockData(column.original(), false);
        covered.remove(column.block());
    }

    /** Instant rollback - plugin shutdown safety. */
    public void restoreNow() {
        if (task != null) task.cancel();
        while (frontier > 0) restore(columns.get(--frontier));
        ACTIVE.remove(this);
    }

    public static void restoreAllNow() {
        for (GroundCover cover : new ArrayList<>(ACTIVE)) cover.restoreNow();
    }

    private static Block topSolid(Location center, int dx, int dz) {
        int x = center.getBlockX() + dx;
        int z = center.getBlockZ() + dz;
        for (int y = center.getBlockY(); y > center.getBlockY() - 8; y--) {
            Block block = center.getWorld().getBlockAt(x, y, z);
            if (block.getType().isSolid()) return block;
        }
        return null;
    }

    private static boolean convertible(Block block) {
        Material type = block.getType();
        if (type == Material.BEDROCK || type == Material.OBSIDIAN) return false;
        if (type.name().contains("SPAWNER") || type.name().contains("SIGN")
                || type.name().contains("BED")) return false;
        BlockState state = block.getState();
        return !(state instanceof TileState);
    }
}
