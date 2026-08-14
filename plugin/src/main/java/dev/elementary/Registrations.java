package dev.elementary;

import dev.elementary.ability.AbilityManager;
import dev.elementary.ability.air.Gale;
import dev.elementary.ability.air.Tempest;
import dev.elementary.ability.air.Updraft;
import dev.elementary.ability.ice.CatchTheRainbow;
import dev.elementary.ability.ice.Mirage;
import dev.elementary.ability.ice.MirageSyncListener;
import dev.elementary.ability.ice.OrbitalIce;
import dev.elementary.ability.ice.PlayerCloneRenderer;
import dev.elementary.ability.ice.StandCloneRenderer;
import dev.elementary.ability.ice.SubZero;
import dev.elementary.ability.earth.Bulwark;
import dev.elementary.ability.light.Flash;
import dev.elementary.ability.lightning.Arc;
import dev.elementary.ability.lightning.ChainLightning;
import dev.elementary.ability.lightning.Supercell;
import dev.elementary.ability.light.SolarFlare;
import dev.elementary.ability.light.Sunspear;
import dev.elementary.ability.shadow.Eclipse;
import dev.elementary.ability.shadow.Grasp;
import dev.elementary.ability.shadow.Shadowstep;
import dev.elementary.ability.earth.Cataclysm;
import dev.elementary.ability.earth.Tremor;
import dev.elementary.ability.fire.FireballAbility;
import dev.elementary.ability.fire.Meteor;
import dev.elementary.ability.fire.Pyre;
import dev.elementary.ability.water.Maelstrom;
import dev.elementary.ability.water.Thunderstorm;
import dev.elementary.ability.water.TidePull;
import dev.elementary.command.AdminCommand;
import dev.elementary.command.BrokerCommand;
import dev.elementary.command.InfoCommand;
import dev.elementary.core.LockdownListener;
import dev.elementary.element.Element;
import dev.elementary.item.BrokerMenu;
import dev.elementary.item.ItemListener;
import dev.elementary.item.Items;
import dev.elementary.listener.CombatListener;
import dev.elementary.passive.PassiveTask;
import dev.elementary.tier.Challenges;
import dev.elementary.tier.TierListener;
import org.bukkit.plugin.PluginManager;

/** One place that wires every listener, kit, task and command. */
final class Registrations {
    private Registrations() {}

    static void registerAll(ElementaryPlugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        AbilityManager abilities = plugin.abilities();

        Items.init(plugin);

        Tremor tremor = new Tremor(plugin);
        Bulwark bulwark = new Bulwark(plugin);
        Cataclysm cataclysm = new Cataclysm(plugin);
        abilities.register(Element.EARTH,
                new AbilityManager.Kit(tremor, bulwark, cataclysm));

        TidePull tidePull = new TidePull(plugin);
        Thunderstorm thunderstorm = new Thunderstorm(plugin);
        Maelstrom maelstrom = new Maelstrom(plugin);
        abilities.register(Element.WATER,
                new AbilityManager.Kit(tidePull, thunderstorm, maelstrom));

        FireballAbility fireball = new FireballAbility(plugin);
        Pyre pyre = new Pyre(plugin);
        Meteor meteor = new Meteor(plugin);
        abilities.register(Element.FIRE,
                new AbilityManager.Kit(fireball, pyre, meteor));

        Updraft updraft = new Updraft(plugin);
        Gale gale = new Gale(plugin);
        Tempest tempest = new Tempest(plugin);
        abilities.register(Element.AIR,
                new AbilityManager.Kit(updraft, gale, tempest));

        boolean stands = "stands".equalsIgnoreCase(
                plugin.getConfig().getString("mirage-clones", "players"));
        Mirage mirage = new Mirage(plugin, null);
        Mirage.CloneRenderer renderer = stands
                ? new StandCloneRenderer() : new PlayerCloneRenderer(plugin);
        mirage.setRenderer(renderer);
        plugin.setMirage(mirage);
        OrbitalIce orbitalIce = new OrbitalIce(plugin);
        SubZero subZero = new SubZero(plugin);
        abilities.register(Element.ICE,
                new AbilityManager.Kit(mirage, orbitalIce, subZero));

        abilities.register(Element.SHADOW, new AbilityManager.Kit(
                new Shadowstep(plugin), new Grasp(plugin), new Eclipse(plugin)));
        abilities.register(Element.LIGHT, new AbilityManager.Kit(
                new Flash(plugin), new Sunspear(plugin), new SolarFlare(plugin)));
        abilities.register(Element.LIGHTNING, new AbilityManager.Kit(
                new Arc(plugin), new ChainLightning(plugin), new Supercell(plugin)));
        plugin.getSLF4JLogger().info("Mirage clones: {}",
                stands ? "armour stands (config)" : "real-skin players");

        Challenges challenges = new Challenges(plugin);
        plugin.setChallenges(challenges);

        pm.registerEvents(new LockdownListener(plugin), plugin);
        pm.registerEvents(abilities, plugin);
        pm.registerEvents(new CombatListener(plugin), plugin);
        pm.registerEvents(bulwark, plugin);
        pm.registerEvents(thunderstorm, plugin);
        pm.registerEvents(fireball, plugin);
        pm.registerEvents(meteor, plugin);
        pm.registerEvents(new CatchTheRainbow(plugin), plugin);
        pm.registerEvents(orbitalIce, plugin);
        pm.registerEvents(new MirageSyncListener(plugin, mirage), plugin);
        pm.registerEvents(challenges, plugin);
        pm.registerEvents(new TierListener(plugin), plugin);
        BrokerMenu brokerMenu = new BrokerMenu(plugin);
        pm.registerEvents(brokerMenu, plugin);
        pm.registerEvents(new ItemListener(plugin, brokerMenu), plugin);

        new PassiveTask(plugin).runTaskTimer(plugin, 20, 20);

        plugin.getCommand("info").setExecutor(new InfoCommand(plugin));
        plugin.getCommand("elementary").setExecutor(new AdminCommand(plugin));
        BrokerCommand brokerCommand = new BrokerCommand(plugin);
        plugin.getCommand("broker").setExecutor(brokerCommand);
        plugin.getCommand("broker").setTabCompleter(brokerCommand);
    }
}
