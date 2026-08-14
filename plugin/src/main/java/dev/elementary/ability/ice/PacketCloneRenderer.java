package dev.elementary.ability.ice;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import dev.elementary.ElementaryPlugin;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Packet-level fake players carrying the caster's own game profile, so
 * the clones wear his actual skin. Spawn, movement and equipment are
 * pure packets; hits arrive as interact packets on the fake ids.
 */
public class PacketCloneRenderer implements Mirage.CloneRenderer {
    private final ElementaryPlugin plugin;

    public PacketCloneRenderer(ElementaryPlugin plugin, Mirage mirageAccessor) {
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
                    @Override
                    public void onPacketReceive(PacketReceiveEvent event) {
                        try {
                            if (event.getPacketType()
                                    != PacketType.Play.Client.INTERACT_ENTITY) {
                                return;
                            }
                            WrapperPlayClientInteractEntity wrapper =
                                    new WrapperPlayClientInteractEntity(event);
                            if (wrapper.getAction()
                                    != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                                return;
                            }
                            int id = wrapper.getEntityId();
                            Player attacker = (Player) event.getPlayer();
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                Mirage mirage = mirageAccessor != null ? mirageAccessor
                                        : plugin.mirage();
                                if (mirage != null) mirage.handleHit(attacker, id);
                            });
                        } catch (Throwable ignored) {
                            // never let a wrapper decode failure into the pipeline
                        }
                    }
                });
    }

    @Override
    public Mirage.Clone spawn(Player owner, Location location) {
        int entityId = SpigotReflectionUtil.generateEntityId();
        UUID uuid = UUID.randomUUID();
        UserProfile profile = new UserProfile(uuid, owner.getName());
        owner.getPlayerProfile().getProperties().forEach(property ->
                profile.getTextureProperties().add(new TextureProperty(
                        property.getName(), property.getValue(), property.getSignature())));

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        profile, false, 0, GameMode.SURVIVAL, null, null);
        broadcast(new WrapperPlayServerPlayerInfoUpdate(EnumSet.of(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER), info));
        broadcast(new WrapperPlayServerSpawnEntity(entityId, Optional.of(uuid),
                EntityTypes.PLAYER,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(), location.getYaw(), location.getYaw(), 0,
                Optional.empty()));
        // show every skin layer
        broadcast(new WrapperPlayServerEntityMetadata(entityId, List.of(
                new EntityData(17, EntityDataTypes.BYTE, (byte) 0x7E))));

        Mirage.Clone clone = new Mirage.Clone() {
            Location current = location.clone();

            @Override public void moveTo(Location to) {
                current = to.clone();
                broadcast(new WrapperPlayServerEntityTeleport(entityId,
                        new Vector3d(to.getX(), to.getY(), to.getZ()),
                        to.getYaw(), to.getPitch(), true));
            }

            @Override public Location location() { return current.clone(); }

            @Override public void pop() {
                broadcast(new WrapperPlayServerDestroyEntities(entityId));
                broadcast(new WrapperPlayServerPlayerInfoRemove(uuid));
            }

            @Override public void syncEquipment() {
                List<Equipment> equipment = new ArrayList<>();
                equipment.add(new Equipment(EquipmentSlot.MAIN_HAND,
                        SpigotConversionUtil.fromBukkitItemStack(
                                owner.getInventory().getItemInMainHand())));
                equipment.add(new Equipment(EquipmentSlot.HELMET,
                        SpigotConversionUtil.fromBukkitItemStack(
                                owner.getInventory().getHelmet())));
                equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE,
                        SpigotConversionUtil.fromBukkitItemStack(
                                owner.getInventory().getChestplate())));
                equipment.add(new Equipment(EquipmentSlot.LEGGINGS,
                        SpigotConversionUtil.fromBukkitItemStack(
                                owner.getInventory().getLeggings())));
                equipment.add(new Equipment(EquipmentSlot.BOOTS,
                        SpigotConversionUtil.fromBukkitItemStack(
                                owner.getInventory().getBoots())));
                broadcast(new WrapperPlayServerEntityEquipment(entityId, equipment));
            }

            @Override public boolean matchesEntityId(int id) { return id == entityId; }
        };
        clone.syncEquipment();
        return clone;
    }

    private void broadcast(com.github.retrooper.packetevents.wrapper.PacketWrapper<?> packet) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }
}
