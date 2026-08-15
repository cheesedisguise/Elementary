package dev.elementary;

import dev.elementary.ability.AbilityManager;
import dev.elementary.ability.air.Gale;
import dev.elementary.ability.air.Tempest;
import dev.elementary.ability.air.Updraft;
import dev.elementary.ability.ice.CatchTheRainbow;
import dev.elementary.ability.ice.FrozenOver;
import dev.elementary.ability.ice.OrbitalIce;
import dev.elementary.ability.ice.SubZero;
import dev.elementary.ability.earth.Boulder;
import dev.elementary.ability.light.Consecrate;
import dev.elementary.ability.lightning.Overcharge;
import dev.elementary.ability.lightning.Supercell;
import dev.elementary.ability.lightning.VoltRush;
import dev.elementary.ability.light.SolarFlare;
import dev.elementary.ability.light.Sunspear;
import dev.elementary.ability.shadow.Eclipse;
import dev.elementary.ability.shadow.MarkForDeath;
import dev.elementary.ability.shadow.Shadowstep;
import dev.elementary.ability.earth.Cataclysm;
import dev.elementary.ability.earth.Fissure;
import dev.elementary.ability.fire.FireballAbility;
import dev.elementary.ability.fire.MeteorShower;
import dev.elementary.ability.fire.Pyre;
import dev.elementary.ability.water.HealingSpring;
import dev.elementary.ability.water.Maelstrom;
import dev.elementary.ability.water.TidePull;
import dev.elementary.command.AdminCommand;
import dev.elementary.command.BrokerCommand;
import dev.elementary.command.InfoCommand;
import dev.elementary.command.TrustCommand;
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

        Fissure fissure = new Fissure(plugin);
        Boulder boulder = new Boulder(plugin);
        Cataclysm cataclysm = new Cataclysm(plugin);
        abilities.register(Element.EARTH,
                new AbilityManager.Kit(fissure, boulder, cataclysm));

        TidePull tidePull = new TidePull(plugin);
        HealingSpring healingSpring = new HealingSpring(plugin);
        Maelstrom maelstrom = new Maelstrom(plugin);
        abilities.register(Element.WATER,
                new AbilityManager.Kit(tidePull, healingSpring, maelstrom));

        FireballAbility fireball = new FireballAbility(plugin);
        Pyre pyre = new Pyre(plugin);
        MeteorShower meteor = new MeteorShower(plugin);
        abilities.register(Element.FIRE,
                new AbilityManager.Kit(fireball, pyre, meteor));

        Updraft updraft = new Updraft(plugin);
        Gale gale = new Gale(plugin);
        Tempest tempest = new Tempest(plugin);
        abilities.register(Element.AIR,
                new AbilityManager.Kit(updraft, gale, tempest));

        FrozenOver frozenOver = new FrozenOver(plugin);
        OrbitalIce orbitalIce = new OrbitalIce(plugin);
        SubZero subZero = new SubZero(plugin);
        abilities.register(Element.ICE,
                new AbilityManager.Kit(frozenOver, orbitalIce, subZero));

        MarkForDeath markForDeath = new MarkForDeath(plugin);
        abilities.register(Element.SHADOW, new AbilityManager.Kit(
                new Shadowstep(plugin), markForDeath, new Eclipse(plugin)));
        Sunspear sunspear = new Sunspear(plugin);
        Consecrate consecrate = new Consecrate(plugin);
        SolarFlare solarFlare = new SolarFlare(plugin);
        abilities.register(Element.LIGHT, new AbilityManager.Kit(
                sunspear, consecrate, solarFlare));
        abilities.register(Element.LIGHTNING, new AbilityManager.Kit(
                new VoltRush(plugin), new Overcharge(plugin), new Supercell(plugin)));

        Challenges challenges = new Challenges(plugin);
        plugin.setChallenges(challenges);

        pm.registerEvents(new LockdownListener(plugin), plugin);
        pm.registerEvents(abilities, plugin);
        pm.registerEvents(new CombatListener(plugin), plugin);
        pm.registerEvents(boulder, plugin);
        pm.registerEvents(fireball, plugin);
        pm.registerEvents(meteor, plugin);
        pm.registerEvents(new CatchTheRainbow(plugin), plugin);
        pm.registerEvents(orbitalIce, plugin);
        pm.registerEvents(consecrate, plugin);
        pm.registerEvents(solarFlare, plugin);
        pm.registerEvents(markForDeath, plugin);
        pm.registerEvents(challenges, plugin);
        pm.registerEvents(new TierListener(plugin), plugin);
        BrokerMenu brokerMenu = new BrokerMenu(plugin);
        pm.registerEvents(brokerMenu, plugin);
        pm.registerEvents(new ItemListener(plugin, brokerMenu), plugin);

        PassiveTask passives = new PassiveTask(plugin);
        plugin.setPassives(passives);
        passives.runTaskTimer(plugin, 20, 20);

        plugin.getCommand("info").setExecutor(new InfoCommand(plugin));
        plugin.getCommand("elementary").setExecutor(new AdminCommand(plugin));
        BrokerCommand brokerCommand = new BrokerCommand(plugin);
        plugin.getCommand("broker").setExecutor(brokerCommand);
        plugin.getCommand("broker").setTabCompleter(brokerCommand);
        TrustCommand trustCommand = new TrustCommand(plugin);
        plugin.getCommand("trust").setExecutor(trustCommand);
        plugin.getCommand("trust").setTabCompleter(trustCommand);
        plugin.getCommand("untrust").setExecutor(trustCommand);
        plugin.getCommand("untrust").setTabCompleter(trustCommand);
    }
}
