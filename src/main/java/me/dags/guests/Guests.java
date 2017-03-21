package me.dags.guests;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author dags <dags@dags.me>
 */

@Plugin(name = "Guests", id = "guests", version = "1.1")
public class Guests
{
    private static final String world = "guests.world.";
    private static final String build = "guests.build.place";
    private static final String destroy = "guests.build.destroy";
    private static final String spawn = "guests.entity.spawn";
    private static final String inventoryModify = "guests.inventory.modify";
    private static final String interactEntity = "guests.interact.entity";
    private static final String interactOpenable = "guests.interact.openable";

    private final Set<UUID> worlds = new HashSet<>();

    @Listener
    public void init(GameInitializationEvent event)
    {
        // load config
    }

    @Listener (order = Order.LAST)
    public void worldLoad(LoadWorldEvent event)
    {
        if (event.getTargetWorld().getName().equalsIgnoreCase("arda"))
        {
            worlds.add(event.getTargetWorld().getUniqueId());
        }
    }

    @Listener(order = Order.PRE)
    public void onTp(MoveEntityEvent.Teleport event, @First Player player)
    {
        World to = event.getToTransform().getExtent();
        if (!player.hasPermission(world + to.getName().toLowerCase()))
        {
            event.setCancelled(true);
            player.setTransform(event.getFromTransform());
            player.sendMessage(Text.of("You do not have permission to enter that world", TextColors.RED));
        }
    }

    @Listener (order = Order.LAST)
    public void changeInventory(ChangeInventoryEvent event, @Root Player player)
    {
        test(player, inventoryModify, event);
    }

    @Listener (order = Order.LAST)
    public void spawn(SpawnEntityEvent event, @Root Player player)
    {
        test(player, spawn, event);
    }

    @Listener (order = Order.LAST)
    public void onEntityInteract(InteractEntityEvent event, @Root Player player)
    {
        test(player, interactEntity, event);
    }

    @Listener (order = Order.LAST)
    public void onBlockInteractPrim(InteractBlockEvent.Primary event, @Root Player player)
    {
        test(player, destroy, event);
    }

    @Listener (order = Order.LAST)
    public void onBlockInteractSec(InteractBlockEvent.Secondary event, @Root Player player)
    {
        BlockSnapshot block = event.getTargetBlock();
        if (block.supports(Keys.OPEN) && (block.supports(Keys.HINGE_POSITION) || block.supports(Keys.IN_WALL)) && player.hasPermission(interactOpenable))
        {
            // allow player to interact with doors and fence-gates
            return;
        }
        test(player, build, event);
    }

    @Listener (order = Order.LAST)
    public void onBlockBreak(ChangeBlockEvent.Break event, @Root Player player)
    {
        test(player, destroy, event);
    }

    @Listener (order = Order.LAST)
    public void onBlockPlace(ChangeBlockEvent.Place event, @Root Player player)
    {
        test(player, build, event);
    }

    private void test(Player player, String permission, Cancellable event)
    {
        if (worlds.contains(player.getWorld().getUniqueId()) && !player.hasPermission(permission))
        {
            event.setCancelled(true);
        }
    }
}
