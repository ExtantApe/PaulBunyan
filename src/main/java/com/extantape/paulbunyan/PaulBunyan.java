package com.extantape.paulbunyan;

import com.extantape.paulbunyan.fall.Journal;
import com.extantape.paulbunyan.hook.BlockLog;
import com.extantape.paulbunyan.hook.Protection;
import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Scan;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

/** Chop a natural tree with an axe and it falls over. */
public final class PaulBunyan extends JavaPlugin {
    private Feller feller;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Settings settings = Settings.read(getConfig(), getLogger());
        Journal journal;
        try {
            journal = new Journal(getDataFolder().toPath().resolve("falls"));
        } catch (IOException e) {
            getLogger().severe("Can't create the fall journal folder, so trees won't fall: " + e);
            return;
        }
        feller = new Feller(this, settings, new PlacedBlocks(this), new Protection(getLogger()), new BlockLog(getLogger()), journal);
        feller.replay();
        getServer().getPluginManager().registerEvents(new ChopListener(feller), this);
        getServer().getScheduler().runTaskTimer(this, feller::forgetOldFalls, 20L * 60, 20L * 60);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> e.registrar().register(
            Commands.literal("paulbunyan")
                .requires(src -> src.getSender().hasPermission("paulbunyan.admin"))
                .then(Commands.literal("reload").executes(this::reload))
                .then(Commands.literal("check")
                    .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                        .executes(c -> check(c, null))
                        .then(Commands.argument("world", ArgumentTypes.world())
                            .executes(c -> check(c, c.getArgument("world", World.class))))))
                .build(),
            "Reload PaulBunyan, or check whether a log would fall.", List.of("pb")));
    }

    @Override
    public void onDisable() {
        if (feller != null) feller.finishAll();
    }

    private int reload(CommandContext<CommandSourceStack> c) {
        reloadConfig();
        feller.reload(Settings.read(getConfig(), getLogger()), new Protection(getLogger()), new BlockLog(getLogger()));
        c.getSource().getSender().sendMessage(Component.text("PaulBunyan reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int check(CommandContext<CommandSourceStack> c, World world) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPosition at = c.getArgument("pos", BlockPositionResolver.class).resolve(c.getSource());
        World w = world != null ? world : c.getSource().getLocation().getWorld();
        Block b = w.getBlockAt(at.blockX(), at.blockY(), at.blockZ());
        Scan s = feller.scan(b, null);
        String where = w.getName() + " " + new Pos(b.getX(), b.getY(), b.getZ());
        Component msg = s.fells()
            ? Component.text("[PaulBunyan] check " + where + ": falls, " + s.tree().species().name().toLowerCase() + ", "
                + s.tree().logs().size() + " logs, " + s.tree().leaves().size() + " leaves, " + s.tree().strays() + " placed logs left standing", NamedTextColor.GREEN)
            : Component.text("[PaulBunyan] check " + where + ": breaks like vanilla, " + s.skip().text
                + (s.detail() == null ? "" : " (" + s.detail() + ")"), NamedTextColor.YELLOW);
        c.getSource().getSender().sendMessage(msg);
        return Command.SINGLE_SUCCESS;
    }
}
