package dev.elementary.trust;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Per-player ally lists. Trust is one-way: your list governs what YOUR
 * abilities do - harmful ones skip your allies, supportive ones include
 * them. It says nothing about what their abilities do to you.
 */
public class TrustService {
    private final JavaPlugin plugin;
    private final File file;
    /** owner -> (trusted uuid -> last known name, for offline listing). */
    private final Map<UUID, Map<UUID, String>> trust = new HashMap<>();

    public TrustService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "trust.yml");
        load();
    }

    public boolean trusts(UUID owner, UUID other) {
        Map<UUID, String> set = trust.get(owner);
        return set != null && set.containsKey(other);
    }

    public void trust(UUID owner, UUID other, String name) {
        trust.computeIfAbsent(owner, k -> new LinkedHashMap<>()).put(other, name);
        save();
    }

    /** Removes by exact name (case-insensitive) or UUID string; null if absent. */
    public String untrust(UUID owner, String who) {
        Map<UUID, String> set = trust.get(owner);
        if (set == null) return null;
        for (Map.Entry<UUID, String> e : set.entrySet()) {
            if (e.getValue().equalsIgnoreCase(who)
                    || e.getKey().toString().equalsIgnoreCase(who)) {
                String name = e.getValue();
                set.remove(e.getKey());
                if (set.isEmpty()) trust.remove(owner);
                save();
                return name;
            }
        }
        return null;
    }

    public Map<UUID, String> trusted(UUID owner) {
        return Map.copyOf(trust.getOrDefault(owner, Map.of()));
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String ownerKey : yml.getKeys(false)) {
            ConfigurationSection s = yml.getConfigurationSection(ownerKey);
            if (s == null) continue;
            try {
                UUID owner = UUID.fromString(ownerKey);
                Map<UUID, String> set = new LinkedHashMap<>();
                for (String key : s.getKeys(false)) {
                    set.put(UUID.fromString(key), s.getString(key, "?"));
                }
                if (!set.isEmpty()) trust.put(owner, set);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<UUID, String>> e : trust.entrySet()) {
            for (Map.Entry<UUID, String> t : e.getValue().entrySet()) {
                yml.set(e.getKey() + "." + t.getKey(), t.getValue());
            }
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getSLF4JLogger().error("Could not save trust.yml", ex);
        }
    }
}
