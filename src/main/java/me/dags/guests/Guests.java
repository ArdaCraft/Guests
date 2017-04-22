package me.dags.guests;

import com.google.common.collect.Sets;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * @author dags <dags@dags.me>
 */

@Plugin(name = "Guests", id = "guests", version = "1.2.1")
public class Guests
{
    private static final String world = "guests.world.";
    private static final String build = "guests.build.place";
    private static final String destroy = "guests.build.destroy";
    private static final String spawn = "guests.entity.spawn";
    private static final String launch = "guests.entity.launch";
    private static final String inventoryModify = "guests.inventory.modify";
    private static final String interactEntity = "guests.interact.entity";
    private static final String interactOpenable = "guests.interact.openable";

    // this sucks ass, but CR blocks don't inherit normal block traits like Keys.OPEN :/
    private static Predicate<BlockState> openable = blockState -> {
        boolean open = false, hinge = false, inWall = false;
        for (BlockTrait trait : blockState.getTraits()) {
            String id = trait.getName();
            open = open || id.equals("open");
            hinge = hinge || id.equals("hinge");
            inWall = inWall || id.equals("in_wall");
        }
        return open && (hinge || inWall);
    };

    private final Set<String> whitelist = Sets.newHashSet("plots");
    private final Set<UUID> protectedWorlds = new HashSet<>();

    @Listener
    public void init(GameInitializationEvent event)
    {
        // load config
    }

    @Listener (order = Order.LAST)
    public void worldLoad(LoadWorldEvent event)
    {
        if (!whitelist.contains(event.getTargetWorld().getName().toLowerCase()))
        {
            protectedWorlds.add(event.getTargetWorld().getUniqueId());
        }
    }

    @Listener(order = Order.POST)
    public void onTp(MoveEntityEvent.Teleport event, @Getter("getTargetEntity") Player player)
    {
        World to = event.getToTransform().getExtent();
        if (!player.hasPermission(world + to.getName().toLowerCase()))
        {
            player.setTransform(event.getFromTransform());
            player.sendMessage(Text.of("You do not have permission to enter that world", TextColors.RED));
        }
    }

    @Listener (order = Order.LAST)
    public void changeInventory(ChangeInventoryEvent.Transfer event, @Root Player player)
    {
        test(player, inventoryModify, event);
    }

    @Listener (order = Order.LAST)
    public void launch(LaunchProjectileEvent event, @First Player player)
    {
        test(player, launch, event);
    }

    @Listener (order = Order.LAST)
    public void spawn(SpawnEntityEvent event, @First Player player)
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
        BlockState block = event.getTargetBlock().getState();
        if (protectedWorlds.contains(player.getWorld().getUniqueId())) {
            if (player.hasPermission(build) || player.hasPermission(interactOpenable) && openable.test(block))
            {
                // allow player to interact with doors and fence-gates
                return;
            }
            event.setCancelled(true);
        }
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
        if (protectedWorlds.contains(player.getWorld().getUniqueId()) && !player.hasPermission(permission))
        {
            event.setCancelled(true);
        }
    }
}
