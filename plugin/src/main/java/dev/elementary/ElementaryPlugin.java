package dev.elementary;

import dev.elementary.ability.AbilityManager;
import dev.elementary.cooldown.Cooldowns;
import dev.elementary.core.LockdownListener;
import dev.elementary.core.ShardService;
import dev.elementary.data.DataStore;
import dev.elementary.element.Element;
import dev.elementary.hud.HudTask;
import dev.elementary.shard.Shards;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class ElementaryPlugin extends JavaPlugin {
    private DataStore store;
    private ShardService shardService;
    private Cooldowns cooldowns;
    private AbilityManager abilities;
    private dev.elementary.trust.TrustService trust;
    private dev.elementary.passive.PassiveTask passives;
    private final Map<UUID, Element> boundPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Shards.init(this);
        loadBoundPlayers();
        store = new DataStore(this);
        shardService = new ShardService(this);
        cooldowns = new Cooldowns();
        abilities = new AbilityManager(this);
        trust = new dev.elementary.trust.TrustService(this);
        dev.elementary.util.Targets.init(trust);

        Registrations.registerAll(this);

        HudTask hud = new HudTask(this);
        getServer().getPluginManager().registerEvents(hud, this);
        hud.runTaskTimer(this, 20, 5);
        getSLF4JLogger().info("Elementary enabled - {} bound player(s)", boundPlayers.size());
    }

    @Override
    public void onDisable() {
        // put every Pyre / Frozen Over floor back before we go
        dev.elementary.util.GroundCover.restoreAllNow();
        if (store != null) store.save();
    }

    public void loadBoundPlayers() {
        boundPlayers.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("bound-players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                boundPlayers.put(UUID.fromString(key),
                        Element.valueOf(section.getString(key, "").toUpperCase()));
            } catch (IllegalArgumentException ex) {
                getSLF4JLogger().warn("bound-players: bad entry {}", key);
            }
        }
    }

    private dev.elementary.tier.Challenges challenges;

    public void setChallenges(dev.elementary.tier.Challenges c) { this.challenges = c; }
    public dev.elementary.tier.Challenges challenges() { return challenges; }

    public DataStore store() { return store; }
    public ShardService shards() { return shardService; }
    public Cooldowns cooldowns() { return cooldowns; }
    public AbilityManager abilities() { return abilities; }
    public dev.elementary.trust.TrustService trust() { return trust; }
    public void setPassives(dev.elementary.passive.PassiveTask p) { this.passives = p; }
    public dev.elementary.passive.PassiveTask passives() { return passives; }
    public Map<UUID, Element> boundPlayers() { return boundPlayers; }
}
