package dev.elementary;

import dev.elementary.ability.AbilityManager;
import dev.elementary.ability.air.Gale;
import dev.elementary.ability.air.Tempest;
import dev.elementary.ability.air.Updraft;
import dev.elementary.ability.aqua.CatchTheRainbow;
import dev.elementary.ability.aqua.Mirage;
import dev.elementary.ability.aqua.MirageSyncListener;
import dev.elementary.ability.aqua.OrbitalIce;
import dev.elementary.ability.aqua.PacketCloneRenderer;
import dev.elementary.ability.aqua.StandCloneRenderer;
import dev.elementary.ability.aqua.SubZero;
import dev.elementary.ability.earth.Bulwark;
import dev.elementary.ability.earth.Cataclysm;
import dev.elementary.ability.earth.Tremor;
import dev.elementary.ability.fire.FireballAbility;
import dev.elementary.ability.fire.Meteor;
import dev.elementary.ability.fire.Pyre;
import dev.elementary.ability.water.Maelstrom;
import dev.elementary.ability.water.Thunderstorm;
import dev.elementary.ability.water.TidePull;
import dev.elementary.command.AdminCommand;
import dev.elementary.command.InfoCommand;
import dev.elementary.core.LockdownListener;
import dev.elementary.element.Element;
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

        boolean packets = pm.getPlugin("packetevents") != null;
        Mirage mirage = new Mirage(plugin, null);
        Mirage.CloneRenderer renderer = packets
                ? new PacketCloneRenderer(plugin, mirage)
                : new StandCloneRenderer();
        mirage.setRenderer(renderer);
        plugin.setMirage(mirage);
        OrbitalIce orbitalIce = new OrbitalIce(plugin);
        SubZero subZero = new SubZero(plugin);
        abilities.register(Element.AQUA,
                new AbilityManager.Kit(mirage, orbitalIce, subZero));
        plugin.getSLF4JLogger().info("Mirage clones: {}",
                packets ? "packet players (PacketEvents)" : "armour-stand fallback");

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
        pm.registerEvents(new ItemListener(plugin), plugin);

        new PassiveTask(plugin).runTaskTimer(plugin, 20, 20);

        plugin.getCommand("info").setExecutor(new InfoCommand(plugin));
        plugin.getCommand("elementary").setExecutor(new AdminCommand(plugin));
    }
}
