package dev.elementary.data;

import dev.elementary.element.Element;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DataStore {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final File file;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    public PlayerData get(UUID id) { return players.get(id); }
    public void put(UUID id, PlayerData data) { players.put(id, data); save(); }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            ConfigurationSection s = yml.getConfigurationSection(key);
            if (s == null) continue;
            try {
                PlayerData d = new PlayerData(Element.valueOf(s.getString("element", "EARTH")));
                d.tier = s.getInt("tier", 1);
                d.challengeProgress = s.getDouble("challenge", 0);
                d.challengeMilestone = s.getInt("milestone", 0);
                d.abilityMessages = s.getBoolean("messages", true);
                players.put(UUID.fromString(key), d);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> e : players.entrySet()) {
            String k = e.getKey().toString();
            PlayerData d = e.getValue();
            yml.set(k + ".element", d.element.name());
            yml.set(k + ".tier", d.tier);
            yml.set(k + ".challenge", d.challengeProgress);
            yml.set(k + ".milestone", d.challengeMilestone);
            yml.set(k + ".messages", d.abilityMessages);
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getSLF4JLogger().error("Could not save players.yml", ex);
        }
    }
}
