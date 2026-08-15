package dev.elementary.ability.shadow;

import dev.elementary.ability.Ability;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * An 8s zone of darkness: enemies wilt inside it, the caster thrives -
 * and while inside, the caster is TRULY invisible: armour, held items,
 * everything. Other clients are simply told the equipment is not there.
 */
public class Eclipse implements Ability {
    private static final EquipmentSlot[] VISIBLE_SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final JavaPlugin plugin;

    public Eclipse(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Eclipse"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double radius = 9;
        caster.getWorld().playSound(center, org.bukkit.Sound.ENTITY_WITHER_AMBIENT,
                1.2f, 0.5f);
        new BukkitRunnable() {
            int ticks = 0;
            double spin = 0;
            boolean cloaked = false;
            @Override public void run() {
                ticks += 2;
                spin += 0.22;
                if (ticks > 20 * 8 || !caster.isOnline()) {
                    if (cloaked && caster.isOnline()) uncloak(caster);
                    cancel();
                    return;
                }
                for (int i = 0; i < 20; i++) {
                    double angle = spin + 2 * Math.PI * i / 20;
                    center.getWorld().spawnParticle(Particle.SQUID_INK,
                            center.clone().add(radius * Math.cos(angle),
                                    0.3 + (i % 4) * 0.7, radius * Math.sin(angle)),
                            1, 0.1, 0.1, 0.1, 0.001);
                }
                center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0),
                        6, radius * 0.5, 1.2, radius * 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(0x4D0F18), 1.6f));
                boolean inside = caster.getLocation().getWorld().equals(center.getWorld())
                        && caster.getLocation().distance(center) <= radius;
                // true invisibility holds only within the dark
                if (inside) {
                    if (ticks % 10 == 0) {
                        caster.addPotionEffect(new PotionEffect(
                                PotionEffectType.INVISIBILITY, 30, 0, true, false));
                        cloak(caster);
                        cloaked = true;
                    }
                } else if (cloaked) {
                    caster.removePotionEffect(PotionEffectType.INVISIBILITY);
                    uncloak(caster);
                    cloaked = false;
                }
                if (ticks % 20 == 0) {
                    for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
                        if (!dev.elementary.util.Targets.hostile(caster, target)) continue;
                        target.damage(1, caster);
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.DARKNESS, 60, 0));
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.WEAKNESS, 40, 1));
                    }
                    if (inside) {
                        caster.addPotionEffect(new PotionEffect(
                                PotionEffectType.STRENGTH, 40, 1, true, false));
                        caster.addPotionEffect(new PotionEffect(
                                PotionEffectType.SPEED, 40, 1, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }

    /** Tell every other client the caster wears and holds nothing. */
    private void cloak(Player caster) {
        Map<EquipmentSlot, ItemStack> bare = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : VISIBLE_SLOTS) {
            bare.put(slot, new ItemStack(Material.AIR));
        }
        for (Player viewer : caster.getWorld().getPlayers()) {
            if (!viewer.equals(caster)) viewer.sendEquipmentChange(caster, bare);
        }
    }

    /** The dark lets go: everyone sees the real gear again. */
    private void uncloak(Player caster) {
        PlayerInventory inv = caster.getInventory();
        Map<EquipmentSlot, ItemStack> real = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : VISIBLE_SLOTS) {
            ItemStack item = inv.getItem(slot);
            real.put(slot, item != null ? item : new ItemStack(Material.AIR));
        }
        for (Player viewer : caster.getWorld().getPlayers()) {
            if (!viewer.equals(caster)) viewer.sendEquipmentChange(caster, real);
        }
    }
}
