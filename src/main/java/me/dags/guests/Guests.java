package me.dags.guests;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import me.dags.commandbus.CommandBus;
import me.dags.commandbus.annotation.Command;
import me.dags.commandbus.annotation.Description;
import me.dags.commandbus.annotation.Permission;
import me.dags.commandbus.annotation.Src;
import me.dags.commandbus.fmt.Fmt;
import me.dags.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandSource;
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

    private static final Logger logger = LoggerFactory.getLogger("GUESTS");

    private static Handler handler = new Handler();
    private static boolean debugEnv = true;
    private static boolean debugUser = true;

    private final Path config;

    @Inject
    public Guests(@ConfigDir(sharedRoot = false) Path path) {
        config = path.resolve("config.conf");
    }

    @Listener
    public void init(GameInitializationEvent event) {
        reload(null);
        CommandBus.create(this).register(this).submit();
        Sponge.getEventManager().registerListeners(this, new Protections());
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

    @Command("guests debug env")
    @Permission("guests.command.debug")
    @Description("Toggle environment protection logging")
    public void toggleEnvDebug(@Src CommandSource source) {
        debugEnv = !debugEnv;
        Fmt.info("Set env debugging: %s", debugEnv).tell(source);
    }

    @Command("guests debug users")
    @Permission("guests.command.debug")
    @Description("Toggle user protection logging")
    public void toggleUserDebug(@Src CommandSource source) {
        debugUser = !debugUser;
        Fmt.info("Set user debugging: %s", debugUser).tell(source);
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

    static void logEnv(String message, Object... args) {
        if (debugEnv) {
            logger.info(message, args);
        }
    }

    static void logUser(String message, Object... args) {
        if (debugUser) {
            logger.info(message, args);
        }
    }
}
