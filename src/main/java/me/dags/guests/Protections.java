package me.dags.guests;

import java.util.function.Predicate;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.storage.WorldProperties;

/**
 * @author dags <dags@dags.me>
 */
public class Protections {

    // catch any block updates that may affect tnt
    private static final Predicate<BlockState> tntNotifyFilter = state -> state.getType() == BlockTypes.TNT;
    // catches block changes from farmland to dirt (ie when farmland is trampled
    private static final Predicate<Transaction<BlockSnapshot>> onPlaceFilter = t -> t.getOriginal().getState().getType() == BlockTypes.FARMLAND && t.getFinal().getState().getType() == BlockTypes.DIRT;
    // catches any living entity spawns that are not Players or ArmourStands
    private static final Predicate<Entity> onSpawnFilter = e -> !(e instanceof Living) || e instanceof Humanoid || e instanceof ArmorStand;

    private final Object plugin;

    public Protections(Object plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onExplosion(ExplosionEvent.Pre event) {
        Guests.logEnv("cancelled explosion event: {}", event.getCause());
        event.setCancelled(true);
    }

    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        if (event.getTargetWorld().getDimension().getType() != DimensionTypes.OVERWORLD) {
            event.getTargetWorld().getProperties().setEnabled(false);
            event.setCancelled(true);
            Sponge.getServer().unloadWorld(event.getTargetWorld());
            Guests.logEnv("unloading world: {}", event.getTargetWorld().getName());
            return;
        }
        Task.builder()
                .delayTicks(1L)
                .execute(() -> {
                    Guests.logEnv("setting world properties: {}", event.getTargetWorld().getName());
                    WorldProperties properties = event.getTargetWorld().getProperties();
                    properties.setDifficulty(Difficulties.PEACEFUL);
                    properties.setGameRule("doDaylightCycle", "false");
                    properties.setGameRule("doWeatherCycle", "false");
                    properties.setGameRule("doFireTick", "false");
                    properties.setGameRule("doEntityDrops", "false");
                    properties.setGameRule("doTileDrops", "false");
                    properties.setGameRule("doMobSpawning", "false");
                    properties.setGameRule("tntexplodes", "false");
                    properties.setGameRule("logAdminCommands", "true");
                    properties.setGameRule("naturalRegeneration", "false");
                    properties.setGameRule("randomTickSpeed", "-1");
                    properties.setGameRule("spawnRadius", "0");
                    properties.setGameRule("disableElytraMovementCheck", "true");
                })
                .submit(plugin);

    }

    @Listener(order = Order.PRE)
    public void onNotifyTNT(NotifyNeighborBlockEvent event) {
        if (event.getNeighbors().values().stream().anyMatch(tntNotifyFilter)) {
            Guests.logEnv("cancelled TNT update");
            event.setCancelled(true);
        }
    }

    // catches any decaying blocks, like leaves
    // decays can't be caused by a player, so it should be safe to block them all
    @Listener(order = Order.PRE)
    public void onDecay(ChangeBlockEvent.Decay event) {
        event.setCancelled(true);
        event.getTransactions().forEach(t -> {
            t.setValid(false);
            Guests.logEnv("cancelled decay: {} -> {}", t.getOriginal().getState().getType(), t.getFinal().getState().getType());
        });
    }

    // catches any growing blocks, like crops or vines
    // players may hav triggered a block to grow using bonemeal, so allow these cases to pass
    @Listener(order = Order.PRE)
    public void onGrow(ChangeBlockEvent.Grow event) {
        if (!event.getCause().containsType(Player.class)) {
            event.setCancelled(true);
            event.getTransactions().forEach(t -> {
                t.setValid(false);
                Guests.logEnv("cancelled grow: {} -> {}", t.getOriginal().getState().getType(), t.getFinal().getState().getType());
            });
        }
    }

    @Listener(order = Order.PRE)
    public void onPlace(ChangeBlockEvent.Place event) {
        event.getTransactions().stream().filter(onPlaceFilter).forEach(t -> {
            Guests.logEnv("cancelled place: {} -> {}", t.getOriginal().getState().getType(), t.getFinal().getState().getType());
            t.setValid(false);
        });
    }

    @Listener(order = Order.PRE)
    public void onSpawn(SpawnEntityEvent event) {
        event.filterEntities(onSpawnFilter).forEach(e -> Guests.logEnv("cancelled entity spawn: {}", e.getType()));
    }
}
