package com.extantape.paulbunyan;

import com.extantape.paulbunyan.fall.Journal;
import com.extantape.paulbunyan.hook.BlockLog;
import com.extantape.paulbunyan.hook.Protection;
import com.extantape.paulbunyan.regrow.Regrowth;
import com.extantape.paulbunyan.regrow.RewildKeeps;
import com.extantape.paulbunyan.PlacedBlocks;
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
    private Regrowth regrowth;
    private DigSpeed digSpeed;
    private com.extantape.paulbunyan.tools.SpecialAxes axes;
    private com.extantape.paulbunyan.tools.Perks perks;
    private com.extantape.paulbunyan.tools.Wedges wedges;
    private java.nio.file.Path packFile;
    private String packSha1;

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
        PlacedBlocks placed = new PlacedBlocks(this);
        FallenLogs fallen = new FallenLogs(this);
        Protection protection = new Protection(getLogger(), getConfig().getStringList("ask-plugins"));
        BlockLog blockLog = new BlockLog(getLogger());
        regrowth = new Regrowth(getDataFolder().toPath(), getLogger(), placed, () -> feller.settings(), protection, blockLog);
        feller = new Feller(this, settings, placed, fallen, protection, blockLog, journal, regrowth);
        feller.replay();
        RewildKeeps keeps = new RewildKeeps(this, w -> List.of(), w -> regrowth.keeps(w));
        getServer().getPluginManager().registerEvents(keeps, this);
        keeps.start();
        getServer().getScheduler().runTaskTimer(this, regrowth::sweep, Regrowth.SWEEP_TICKS, Regrowth.SWEEP_TICKS);
        getServer().getPluginManager().registerEvents(new ChopListener(feller, new com.extantape.paulbunyan.tools.Bucking(this, fallen)), this);
        digSpeed = new DigSpeed(this, () -> feller.settings().fallenLogSpeed(), fallen, feller::axeKind);
        getServer().getPluginManager().registerEvents(digSpeed, this);
        writePack();
        if (settings.specialAxes()) {
            axes = new com.extantape.paulbunyan.tools.SpecialAxes(this, settings.usePack());
            perks = new com.extantape.paulbunyan.tools.Perks(this);
            feller.tools(axes, perks);
            axes.registerRecipes();
            wedges = new com.extantape.paulbunyan.tools.Wedges(this, axes);
            wedges.feller(feller);
            feller.wedges(wedges);
            var pm = getServer().getPluginManager();
            pm.registerEvents(wedges, this);
            pm.registerEvents(new com.extantape.paulbunyan.tools.Saw(axes, fallen, feller), this);
            pm.registerEvents(new com.extantape.paulbunyan.tools.Peavey(axes, fallen, feller), this);
            pm.registerEvents(new com.extantape.paulbunyan.tools.RecipeGuard(axes), this);
            getServer().getScheduler().runTaskTimer(this, wedges::sweep, 20L * 60, 20L * 60);
            getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                    e.getPlayer().discoverRecipes(axes.recipes());
                }
            }, this);
            getServer().getScheduler().runTaskTimer(this, new com.extantape.paulbunyan.tools.FallPreview(feller),
                com.extantape.paulbunyan.tools.FallPreview.PERIOD, com.extantape.paulbunyan.tools.FallPreview.PERIOD);
        }
        if (getConfig().getBoolean("gsit-sideways-logs-only", true)) com.extantape.paulbunyan.hook.SittingLogs.register(this, getLogger());
        getServer().getScheduler().runTaskTimer(this, feller::forgetOldFalls, 20L * 60, 20L * 60);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> e.registrar().register(
            Commands.literal("paulbunyan")
                .requires(src -> src.getSender().hasPermission("paulbunyan.admin"))
                .then(Commands.literal("reload").executes(this::reload))
                .then(Commands.literal("pack").executes(this::pack))
                .then(Commands.literal("give")
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.literal("tool")
                            .then(Commands.argument("tool", StringArgumentType.word()).suggests((c, b) -> suggest(b, com.extantape.paulbunyan.tools.Tool.values()))
                                .executes(this::giveTool)))
                        .then(Commands.argument("handle", StringArgumentType.word()).suggests((c, b) -> suggest(b, com.extantape.paulbunyan.tools.Handle.values()))
                            .then(Commands.argument("head", StringArgumentType.word()).suggests((c, b) -> suggest(b, com.extantape.paulbunyan.tools.Head.values()))
                                .executes(this::give)))))
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
        if (digSpeed != null) digSpeed.clearAll(getServer().getOnlinePlayers());
        if (perks != null) perks.clear();
        if (wedges != null) wedges.returnAll();
        if (axes != null) axes.unregisterRecipes();
        if (feller != null) feller.finishAll();
        if (regrowth != null) regrowth.flush();
    }

    private int reload(CommandContext<CommandSourceStack> c) {
        reloadConfig();
        Protection protection = new Protection(getLogger(), getConfig().getStringList("ask-plugins"));
        BlockLog blockLog = new BlockLog(getLogger());
        feller.reload(Settings.read(getConfig(), getLogger()), protection, blockLog);
        regrowth.reload(protection, blockLog);
        c.getSource().getSender().sendMessage(Component.text("PaulBunyan reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    /** Writes the resource pack to plugins/PaulBunyan/pack, when it's changed, and notes its SHA-1. */
    private void writePack() {
        try {
            var built = com.extantape.paulbunyan.tools.PackBuilder.build();
            packFile = getDataFolder().toPath().resolve("pack").resolve("PaulBunyan-pack.zip");
            java.nio.file.Files.createDirectories(packFile.getParent());
            if (!java.nio.file.Files.exists(packFile) || !java.util.Arrays.equals(java.nio.file.Files.readAllBytes(packFile), built.zip()))
                java.nio.file.Files.write(packFile, built.zip());
            packSha1 = built.sha1();
        } catch (IOException | RuntimeException e) {
            getLogger().warning("Couldn't write the resource pack: " + e);
        }
    }

    private int pack(CommandContext<CommandSourceStack> c) {
        String msg = packSha1 == null ? "The resource pack couldn't be written; see the server log."
            : "[PaulBunyan] pack: " + packFile + ", SHA-1 " + packSha1 + ". Serve it with Packmule (see the README).";
        c.getSource().getSender().sendMessage(Component.text(msg, packSha1 == null ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(
            com.mojang.brigadier.suggestion.SuggestionsBuilder b, Enum<?>[] values) {
        for (Enum<?> v : values) {
            String id = v.name().toLowerCase(java.util.Locale.ROOT);
            if (id.startsWith(b.getRemainingLowerCase())) b.suggest(id);
        }
        return b.buildFuture();
    }

    private int give(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var sender = c.getSource().getSender();
        if (axes == null) {
            sender.sendMessage(Component.text("Special axes are off (tools.special-axes in config.yml).", NamedTextColor.YELLOW));
            return 0;
        }
        var handle = com.extantape.paulbunyan.tools.Handle.byId(c.getArgument("handle", String.class));
        var head = com.extantape.paulbunyan.tools.Head.byId(c.getArgument("head", String.class));
        if (handle == null || head == null) {
            sender.sendMessage(Component.text("No such " + (handle == null ? "handle wood" : "head") + ". Press Tab for the list.", NamedTextColor.YELLOW));
            return 0;
        }
        var player = c.getArgument("player", io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver.class)
            .resolve(c.getSource()).getFirst();
        var item = axes.make(new com.extantape.paulbunyan.tools.AxeKind(handle, head));
        for (var left : player.getInventory().addItem(item).values()) player.getWorld().dropItem(player.getLocation(), left);
        sender.sendMessage(Component.text("[PaulBunyan] gave " + player.getName() + " a " + head.title().toLowerCase() + " axe with a "
            + handle.title().toLowerCase() + " handle.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int giveTool(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var sender = c.getSource().getSender();
        var tool = com.extantape.paulbunyan.tools.Tool.byId(c.getArgument("tool", String.class));
        if (axes == null || tool == null) {
            sender.sendMessage(Component.text(axes == null ? "PaulBunyan's tools are off (tools.special-axes in config.yml)." : "No such tool. Press Tab for the list.", NamedTextColor.YELLOW));
            return 0;
        }
        var player = c.getArgument("player", io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver.class)
            .resolve(c.getSource()).getFirst();
        for (var left : player.getInventory().addItem(axes.make(tool)).values()) player.getWorld().dropItem(player.getLocation(), left);
        sender.sendMessage(Component.text("[PaulBunyan] gave " + player.getName() + " a " + tool.title.toLowerCase() + ".", NamedTextColor.GREEN));
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
