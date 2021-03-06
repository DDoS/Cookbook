package org.spongepowered.cookbook.plugin;

import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.seq;
import static org.spongepowered.api.command.args.GenericArguments.world;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.extra.skylands.SkylandsWorldGeneratorModifier;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.GeneratorTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Plugin(id = "worldstest", name = "WorldsTest", version = "0.2")
public class WorldsTest {

    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                        .description(Text.of("Teleports a player to another world"))
                        .arguments(seq(playerOrSource(Text.of("target")), onlyOne(world(Text.of("world")))))
                        .permission("worldstest.command.tpworld")
                        .executor((src, args) -> {
                            final Optional<WorldProperties> optWorldProperties = args.getOne("world");
                            final Optional<World> optWorld = Sponge.getServer().getWorld(optWorldProperties.get().getWorldName());
                            if (!optWorld.isPresent()) {
                                throw new CommandException(Text.of("World [", Text.of(TextColors.AQUA, optWorldProperties.get().getWorldName()),
                                        "] "
                                        + "was not found."));
                            }
                            for (Player target : args.<Player>getAll("target")) {
                                target.setLocation(new Location<>(optWorld.get(), optWorld.get().getProperties()
                                        .getSpawnPosition()));
                            }
                            return CommandResult.success();
                        })
                        .build()
                , "tpworld");

        Sponge.getCommandManager().register(this, new CommandSpawn(), "spawn");
    }

    @Listener
    public void onServerAboutToStart(GameAboutToStartServerEvent e) {
        final SkylandsWorldGeneratorModifier skylandsModifier = new SkylandsWorldGeneratorModifier();
        Sponge.getRegistry().register(WorldGeneratorModifier.class, skylandsModifier);

        final WorldCreationSettings.Builder builder = WorldCreationSettings.builder();

        builder
                .name("end")
                .enabled(true)
                .loadsOnStartup(true)
                .keepsSpawnLoaded(true)
                .dimension(DimensionTypes.THE_END)
                .generator(GeneratorTypes.THE_END)
                .gameMode(GameModes.CREATIVE);

        createAndLoadWorld(builder.build());

        builder.reset()
                .name("nether")
                .enabled(true)
                .loadsOnStartup(true)
                .keepsSpawnLoaded(true)
                .dimension(DimensionTypes.NETHER)
                .generator(GeneratorTypes.NETHER)
                .gameMode(GameModes.CREATIVE);

        createAndLoadWorld(builder.build());

        builder.reset()
                .name("skylands")
                .enabled(true)
                .loadsOnStartup(true)
                .keepsSpawnLoaded(true)
                .dimension(DimensionTypes.OVERWORLD)
                .generator(GeneratorTypes.OVERWORLD)
                .generatorModifiers(skylandsModifier)
                .gameMode(GameModes.CREATIVE);

        createAndLoadWorld(builder.build());

        builder.reset()
                .name("skyhell")
                .enabled(true)
                .loadsOnStartup(true)
                .keepsSpawnLoaded(true)
                .dimension(DimensionTypes.NETHER)
                .generator(GeneratorTypes.NETHER)
                .generatorModifiers(skylandsModifier)
                .gameMode(GameModes.CREATIVE);

        createAndLoadWorld(builder.build());

        builder.reset()
                .name("skyend")
                .enabled(true)
                .loadsOnStartup(true)
                .keepsSpawnLoaded(true)
                .dimension(DimensionTypes.THE_END)
                .generator(GeneratorTypes.THE_END)
                .generatorModifiers(skylandsModifier)
                .gameMode(GameModes.CREATIVE);

        createAndLoadWorld(builder.build());
    }

    private void createAndLoadWorld(WorldCreationSettings settings) {
        final Optional<WorldProperties> optWorldProperties = Sponge.getServer().createWorldProperties(settings);
        if (optWorldProperties.isPresent()) {
            Sponge.getServer().loadWorld(optWorldProperties.get());
        }
    }

    private static class CommandSpawn implements CommandCallable {

        @Override
        public boolean testPermission(CommandSource source) {
            return source.hasPermission("command.sponge.spawn");
        }

        @Override
        public Optional<Text> getShortDescription(CommandSource source) {
            return Optional.of((Text) Text.of("Used to get spawn information or set the point of a world"));
        }

        @Override
        public Optional<Text> getHelp(CommandSource source) {
            return Optional.of((Text) Text.of("usage: spawn -i <world_name> | -s <world_name> <x> <y> <z>"));
        }

        @Override
        public Text getUsage(CommandSource source) {
            return Text.of("usage: spawn -i <world_name> | -s <world_name> <x> <y> <z>");
        }

        @Override
        public CommandResult process(CommandSource source, String arguments) throws CommandException {
            if (source instanceof Player) {
                if (!testPermission(source)) {
                    return CommandResult.empty();
                }
                final Player player = (Player) source;
                final String[] args = arguments.split(" ");
                World world;
                Vector3i spawnCoordinates;

                // (Player) /spawn
                if (arguments.isEmpty()) {
                    world = player.getWorld();
                    spawnCoordinates = player.getWorld().getProperties().getSpawnPosition();
                    player.setLocation(new Location<>(world, spawnCoordinates.toDouble()));
                } else {
                    if ("-i".equals(args[0])) {
                        // (Player) /spawn -i
                        if (args.length == 1) {
                            world = player.getWorld();
                            // (Player) /spawn -i <world_name>
                        } else {
                            final Optional<World> optWorldCandidate = Sponge.getServer().getWorld(args[1]);
                            if (optWorldCandidate.isPresent()) {
                                world = optWorldCandidate.get();
                            } else {
                                source.sendMessage(Text.of("World [", TextColors.AQUA, args[1], TextColors.WHITE, "] was not found"));
                                return CommandResult.success();
                            }
                        }

                        spawnCoordinates = world.getProperties().getSpawnPosition();

                        source.sendMessage(Text.of("World [", TextColors.AQUA, world.getName(), TextColors.WHITE,
                                "]: spawn -> x [", TextColors.GREEN, spawnCoordinates.getX(), TextColors.WHITE, "] | y [", TextColors.GREEN,
                                spawnCoordinates.getY(), TextColors.WHITE, "] | z [", TextColors.GREEN, spawnCoordinates.getZ(), TextColors
                                        .WHITE, "]."));
                    } else if ("-s".equals(args[0])) {
                        world = player.getWorld();

                        // (Player) /spawn -s <no args>
                        if (args.length == 1) {
                            spawnCoordinates = player.getLocation().getBlockPosition();
                            // (Player) /spawn -s x y z
                        } else {
                            try {
                                spawnCoordinates = new Vector3i(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                            } catch (Exception ex) {
                                source.sendMessage(Text.of("Invalid spawn coordinates, must be in numeric format. Ex. /spawn -s 0 0 0."));
                                return CommandResult.success();
                            }
                        }

                        world.getProperties().setSpawnPosition(spawnCoordinates);

                        source.sendMessage(Text.of("World [", TextColors.AQUA, world.getName(), TextColors.WHITE,
                                "]: spawn set to -> x [", TextColors.GREEN, spawnCoordinates.getX(), TextColors.WHITE, "] | y [", TextColors
                                        .GREEN,
                                spawnCoordinates.getY(), TextColors.WHITE, "] | z [", TextColors.GREEN, spawnCoordinates.getZ(), TextColors
                                        .WHITE, "]."));
                    }
                }
            }
            return CommandResult.success();
        }

        @Override
        public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
            return new ArrayList<>();
        }
    }
}
