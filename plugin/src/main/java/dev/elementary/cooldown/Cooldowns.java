package dev.elementary.cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cooldowns {
    public record Entry(long endMs, long totalMs) {}

    private final Map<UUID, Map<String, Entry>> map = new HashMap<>();

    public boolean ready(UUID id, String key) {
        return remainingMs(id, key) <= 0;
    }

    public long remainingMs(UUID id, String key) {
        Entry e = map.getOrDefault(id, Map.of()).get(key);
        return e == null ? 0 : Math.max(0, e.endMs() - System.currentTimeMillis());
    }

    public void set(UUID id, String key, double seconds) {
        long total = (long) (seconds * 1000);
        map.computeIfAbsent(id, k -> new HashMap<>())
                .put(key, new Entry(System.currentTimeMillis() + total, total));
    }

    /** The entry closest to finishing but not done, for the HUD bar. */
    public Entry mostRecent(UUID id) {
        Entry best = null;
        for (Entry e : map.getOrDefault(id, Map.of()).values()) {
            if (e.endMs() <= System.currentTimeMillis()) continue;
            if (best == null || e.endMs() > best.endMs()) best = e;
        }
        return best;
    }

    public Map<String, Entry> all(UUID id) {
        return map.getOrDefault(id, Map.of());
    }
}
