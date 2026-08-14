package dev.elementary.ability.ice;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Fallback renderer when PacketEvents is absent: an armour-stand
 * mannequin wearing the caster's head, armour and held item. Not a
 * perfect double, but it walks the walk and pops into cloud.
 */
public class StandCloneRenderer implements Mirage.CloneRenderer {
    @Override
    public Mirage.Clone spawn(Player owner, Location location) {
        ArmorStand stand = owner.getWorld().spawn(location, ArmorStand.class, s -> {
            s.setBasePlate(false);
            s.setArms(true);
            s.setPersistent(false);
            s.setInvulnerable(false);
        });
        Mirage.Clone clone = new Mirage.Clone() {
            @Override public void moveTo(Location to) { stand.teleport(to); }
            @Override public Location location() { return stand.getLocation(); }
            @Override public void pop() { stand.remove(); }
            @Override public void syncEquipment() { dress(stand, owner); }
            @Override public boolean matchesEntityId(int entityId) {
                return stand.getEntityId() == entityId;
            }
        };
        clone.syncEquipment();
        return clone;
    }

    private void dress(ArmorStand stand, Player owner) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(owner);
        head.setItemMeta(meta);
        stand.getEquipment().setHelmet(head);
        stand.getEquipment().setChestplate(copy(owner.getInventory().getChestplate()));
        stand.getEquipment().setLeggings(copy(owner.getInventory().getLeggings()));
        stand.getEquipment().setBoots(copy(owner.getInventory().getBoots()));
        stand.getEquipment().setItemInMainHand(copy(owner.getInventory().getItemInMainHand()));
    }

    private ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
