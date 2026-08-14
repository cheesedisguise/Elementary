package dev.elementary.ability.ice;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import dev.elementary.ElementaryPlugin;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

/**
 * Real-skin clones with no external dependency: a fake ServerPlayer is
 * shown to every client through the server's own packet classes (they
 * can't mismatch the server's own protocol), while an invisible
 * Interaction entity rides along as the hitbox so attacks arrive as
 * ordinary Bukkit events.
 */
public class PlayerCloneRenderer implements Mirage.CloneRenderer {
    private static final byte ALL_SKIN_LAYERS = 0x7E;
    private static final int SKIN_LAYERS_INDEX = 17;

    private final ElementaryPlugin plugin;

    public PlayerCloneRenderer(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Mirage.Clone spawn(Player owner, Location location) {
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile profile = new GameProfile(UUID.randomUUID(), owner.getName());
        for (com.destroystokyo.paper.profile.ProfileProperty property
                : owner.getPlayerProfile().getProperties()) {
            if ("textures".equals(property.getName())) {
                profile.properties().put("textures",
                        new com.mojang.authlib.properties.Property("textures",
                                property.getValue(), property.getSignature()));
            }
        }
        ServerPlayer npc = new ServerPlayer(level.getServer(), level, profile,
                ClientInformation.createDefault());
        npc.snapTo(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());

        // build the info entry directly: the fake player has no network
        // connection, and the ServerPlayer-based constructor dereferences it
        broadcast(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                List.of(new ClientboundPlayerInfoUpdatePacket.Entry(
                        profile.id(), profile, false, 0,
                        net.minecraft.world.level.GameType.SURVIVAL,
                        null, false, 0, null))));
        broadcast(new ClientboundAddEntityPacket(npc.getId(), profile.id(),
                location.getX(), location.getY(), location.getZ(),
                location.getPitch(), location.getYaw(),
                EntityType.PLAYER, 0, Vec3.ZERO, location.getYaw()));
        broadcast(new ClientboundSetEntityDataPacket(npc.getId(),
                List.of(SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(SKIN_LAYERS_INDEX,
                                EntityDataSerializers.BYTE), ALL_SKIN_LAYERS))));

        Interaction hitbox = location.getWorld().spawn(location, Interaction.class, i -> {
            i.setInteractionWidth(0.8f);
            i.setInteractionHeight(1.9f);
            i.setResponsive(true);
            i.setPersistent(false);
        });

        Mirage.Clone clone = new Mirage.Clone() {
            Location current = location.clone();

            @Override public void moveTo(Location to) {
                current = to.clone();
                broadcast(new ClientboundEntityPositionSyncPacket(npc.getId(),
                        new PositionMoveRotation(
                                new Vec3(to.getX(), to.getY(), to.getZ()),
                                Vec3.ZERO, to.getYaw(), to.getPitch()),
                        true));
                broadcast(new ClientboundRotateHeadPacket(npc,
                        (byte) (to.getYaw() * 256f / 360f)));
                hitbox.teleport(to);
            }

            @Override public Location location() { return current.clone(); }

            @Override public void pop() {
                broadcast(new ClientboundRemoveEntitiesPacket(npc.getId()));
                broadcast(new ClientboundPlayerInfoRemovePacket(List.of(profile.id())));
                hitbox.remove();
            }

            @Override public void syncEquipment() {
                List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> gear =
                        new ArrayList<>();
                gear.add(Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(
                        owner.getInventory().getItemInMainHand())));
                gear.add(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(
                        owner.getInventory().getHelmet())));
                gear.add(Pair.of(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(
                        owner.getInventory().getChestplate())));
                gear.add(Pair.of(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(
                        owner.getInventory().getLeggings())));
                gear.add(Pair.of(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(
                        owner.getInventory().getBoots())));
                broadcast(new ClientboundSetEquipmentPacket(npc.getId(), gear));
            }

            @Override public boolean matchesEntityId(int entityId) {
                return entityId == hitbox.getEntityId();
            }
        };
        clone.syncEquipment();
        return clone;
    }

    private void broadcast(Packet<?> packet) {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            ((CraftPlayer) viewer).getHandle().connection.send(packet);
        }
    }
}
