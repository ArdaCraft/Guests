package me.dags.guests;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import me.dags.config.Config;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(name = "Guests", id = "guests", version = "2.0.0", description = "sh")
public class Guests {

    private static Handler handler = new Handler();

    private final Path config;

    @Inject
    public Guests(@ConfigDir(sharedRoot = false) Path path) {
        config = path.resolve("config.conf");
    }

    @Listener
    public void init(GameInitializationEvent event) {
        reload(null);
    }

    @Listener
    public void reload(GameReloadEvent event) {
        Object plugin = this;
        Predicate<BlockState> openable = findOpenableBlocks();
        Task.builder().async().execute(() -> {
            Handler handler = reloadHandler(config, openable);
            Task.builder().execute(() -> {
                Sponge.getEventManager().unregisterListeners(Guests.handler);
                Sponge.getEventManager().registerListeners(plugin, Guests.handler = handler);
            }).submit(plugin);
        }).submit(plugin);
    }

    private static Handler reloadHandler(Path path, Predicate<BlockState> openable) {
        WorldRules global = new WorldRules("*", openable);
        ImmutableMap.Builder<String, WorldRules> builder = ImmutableMap.builder();

        Config config = Config.must(path);
        config.iterate((key, node) -> {
            String world = key.toString();
            WorldRules rules = new WorldRules(world, openable);
            rules.read(node);
            builder.put(world, rules);
        });

        Map<String, WorldRules> worlds = builder.build();
        global = worlds.getOrDefault("*", global);
        global.write(config.node("*"));
        config.save();

        return new Handler(global, worlds);
    }

    private static Predicate<BlockState> findOpenableBlocks() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        for (BlockState state : Sponge.getRegistry().getAllOf(BlockState.class)) {
            boolean open = state.getTrait("open").isPresent();
            boolean hinge = state.getTrait("hinge").isPresent();
            boolean inWall = state.getTrait("in_wall").isPresent();
            if (open && (hinge || inWall)) {
                builder.add(state);
            }
        }
        Set<BlockState> states = builder.build();
        return states::contains;
    }
}
