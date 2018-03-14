package me.dags.guests;

import java.util.function.Predicate;
import me.dags.config.Node;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

/**
 * @author dags <dags@dags.me>
 */
public class WorldRules {

    private static final String WORLD = "guests.world.";
    private static final String BUILD = "guests.build.place";
    private static final String BREAK = "guests.build.break";
    private static final String SPAWN = "guests.entity.spawn";
    private static final String LAUNCH = "guests.entity.launch";
    private static final String INTERACT_ENTITY = "guests.interact.entity";
    private static final String INTERACT_OPENABLE = "guests.interact.openable";
    private static final String INTERACT_INVENTORY = "guests.inventory.modify";

    private final Predicate<BlockState> openable;
    private boolean enterWorld = false;
    private boolean placeBlock = false;
    private boolean breakBlock = false;
    private boolean spawnEntity = false;
    private boolean launchProjectile = false;
    private boolean interactEntity = false;
    private boolean interactOpenable = false;
    private boolean interactInventory = false;
    private double velocity2 = 0D;

    public WorldRules(Predicate<BlockState> openable) {
        this.openable = openable;
    }

    public void read(Node node) {
        enterWorld = node.get("enter", false);
        placeBlock = node.get("place", false);
        breakBlock = node.get("break", false);
        spawnEntity = node.get("spawn", false);
        launchProjectile = node.get("launch", false);
        interactEntity = node.get("damage", false);
        interactOpenable = node.get("open", false);
        interactInventory = node.get("inventory", false);
        double velocity = node.get("velocity", 0D);
        velocity2 = velocity * velocity;
    }

    public void write(Node node) {
        node.set("enter", enterWorld);
        node.set("place", placeBlock);
        node.set("break", breakBlock);
        node.set("spawn", spawnEntity);
        node.set("launch", launchProjectile);
        node.set("damage", interactEntity);
        node.set("open", interactOpenable);
        node.set("inventory", interactInventory);
        node.set("velocity", Math.sqrt(velocity2));
    }

    public void move(MoveEntityEvent event, double velocity2) {
        if (this.velocity2 > 0 && velocity2 > this.velocity2) {
            event.setCancelled(true);
            event.setToTransform(event.getFromTransform());
        }
    }

    public void teleport(MoveEntityEvent.Teleport event) {
        if (enterWorld) {
            return;
        }

        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getTargetEntity();
        World to = event.getToTransform().getExtent();

        if (!player.hasPermission(WORLD + to.getName().toLowerCase())) {
            event.setCancelled(true);
            player.sendMessage(Text.of("You do not have permission to enter that world", TextColors.RED));
        }
    }

    public void inventory(ChangeInventoryEvent.Transfer event, Player player) {
        if (interactInventory) {
            return;
        }

        test(player, INTERACT_INVENTORY, event);
    }

    public void launch(LaunchProjectileEvent event, Player player) {
        if (launchProjectile) {
            return;
        }

        test(player, LAUNCH, event);
    }

    public void spawn(SpawnEntityEvent event, Player player) {
        if (spawnEntity) {
            return;
        }

        test(player, SPAWN, event);
    }

    public void interactEntity(InteractEntityEvent event, Player player) {
        if (interactEntity) {
            return;
        }

        test(player, INTERACT_ENTITY, event);
    }

    public void breakBlock(ChangeBlockEvent.Break event, Player player) {
        if (breakBlock) {
            return;
        }

        test(player, BREAK, event);
    }

    public void interactBlockPrim(InteractBlockEvent.Primary event, Player player) {
        if (breakBlock) {
            return;
        }

        test(player, BREAK, event);
    }

    public void interactBlockSec(InteractBlockEvent.Secondary event, Player player) {
        if (interactOpenable) {
            return;
        }

        BlockState block = event.getTargetBlock().getState();
        if (player.hasPermission(BUILD) || player.hasPermission(INTERACT_OPENABLE) && openable.test(block)) {
            return;
        }

        event.setCancelled(true);
    }

    public void placeBlock(ChangeBlockEvent.Place event, Player player) {
        if (placeBlock) {
            return;
        }

        test(player, BUILD, event);
    }

    private void test(Player player, String permission, Cancellable event) {
        if (!player.hasPermission(permission)) {
            event.setCancelled(true);
        }
    }
}
