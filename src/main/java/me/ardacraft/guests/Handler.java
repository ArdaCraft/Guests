package me.ardacraft.guests;

import java.util.Collections;
import java.util.Map;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.world.World;

/**
 * @author dags <dags@dags.me>
 */
public class Handler {

    private final WorldRules global;
    private final Map<String, WorldRules> worlds;

    public Handler() {
        this(new WorldRules("#none", s -> true), Collections.emptyMap());
    }

    public Handler(WorldRules global, Map<String, WorldRules> worlds) {
        this.global = global;
        this.worlds = worlds;
    }

    private WorldRules getRules(String world) {
        return worlds.getOrDefault(world, global);
    }

    private WorldRules getRules(World world) {
        return getRules(world.getName());
    }

    @Listener(order = Order.POST)
    public void onTeleport(MoveEntityEvent.Teleport event) {
        getRules(event.getToTransform().getExtent()).teleport(event);
    }

    @Listener(order = Order.LAST)
    public void onChangeInventory(ChangeInventoryEvent.Transfer event, @First Player player) {
        getRules(player.getWorld()).inventory(event, player);
    }

    @Listener(order = Order.LAST)
    public void onLaunch(LaunchProjectileEvent event, @First Player player) {
        getRules(player.getWorld()).launch(event, player);
    }

    @Listener(order = Order.LAST)
    public void onSpawn(SpawnEntityEvent event, @First Player player) {
        getRules(player.getWorld()).spawn(event, player);
    }

    @Listener(order = Order.LAST)
    public void onInteractEntity(InteractEntityEvent event, @First Player player) {
        getRules(player.getWorld()).interactEntity(event, player);
    }

    @Listener(order = Order.LAST)
    public void onInteractBlockPrim(InteractBlockEvent.Primary event, @First Player player) {
        getRules(player.getWorld()).interactBlockPrim(event, player);
    }

    @Listener(order = Order.LAST)
    public void onInteractBlockSec(InteractBlockEvent.Secondary event, @First Player player) {
        getRules(player.getWorld()).interactBlockSec(event, player);
    }

    @Listener(order = Order.LAST)
    public void onBreakBlock(ChangeBlockEvent.Break event, @First Player player) {
        getRules(player.getWorld()).breakBlock(event, player);
    }

    @Listener(order = Order.LAST)
    public void onPlaceBlock(ChangeBlockEvent.Place event, @First Player player) {
        getRules(player.getWorld()).placeBlock(event, player);
    }
}
