package com.extantape.paulbunyan.hook;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** GriefPrevention installed but impossible to ask means no tree falls anywhere. */
class ProtectionTest {
    final List<String> logged = new ArrayList<>();
    final Logger log = Logger.getAnonymousLogger();
    { log.setUseParentHandlers(false); log.addHandler(new Handler() {
        @Override public void publish(LogRecord r) { logged.add(r.getMessage()); }
        @Override public void flush() {}
        @Override public void close() {}
    }); }

    @SuppressWarnings("unchecked")
    static <T> T stub(Class<T> type, boolean enabled) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, m, args) -> switch (m.getName()) {
            case "isEnabled" -> enabled;
            case "getName" -> "GriefPrevention";
            case "hashCode" -> 1;
            case "equals" -> proxy == args[0];
            default -> null;
        });
    }

    @Test void controlWithoutGriefPreventionEverythingMayFall() {
        Protection p = new Protection(null, log);
        assertFalse(p.present());
        assertTrue(p.mayBreak(stub(Player.class, true), null));
        assertTrue(p.mayBuild(UUID.randomUUID(), null));
    }

    @Test void aGriefPreventionThatCantBeAskedSaysNoAndSaysWhy() {
        // A plugin called GriefPrevention whose claim check isn't where it should be.
        Protection p = new Protection(stub(Plugin.class, true), log);
        assertTrue(p.present());
        assertFalse(p.mayBreak(stub(Player.class, true), null), "no tree falls");
        assertFalse(p.mayBuild(null, null), "no log lands");
        assertTrue(logged.stream().anyMatch(m -> m.contains("Trees won't fall at all until this is fixed")), String.valueOf(logged));
    }

    @Test void aDisabledGriefPreventionStillCountsAsPresentAndSaysNo() {
        Protection p = new Protection(stub(Plugin.class, false), log);
        assertTrue(p.present());
        assertFalse(p.mayBreak(stub(Player.class, true), null));
    }

    @Test void noGriefPreventionMeansNothingIsClaimed() {
        assertFalse(new Protection(null, log).claimed(null));
    }

    @Test void aGriefPreventionThatCantBeAskedCountsEverythingAsClaimed() {
        // so nothing grows into what might be somebody's claim
        assertTrue(new Protection(stub(Plugin.class, true), log).claimed(null));
        assertTrue(new Protection(stub(Plugin.class, false), log).claimed(null));
    }

    // ---------- ask-plugins: plugins without a claim API, like Waycave's gates ----------

    final List<RegisteredListener> registered = new ArrayList<>();

    @AfterEach void unregister() {
        for (RegisteredListener l : registered) BlockBreakEvent.getHandlerList().unregister(l);
    }

    @SuppressWarnings("unchecked")
    static Plugin named(String name, boolean enabled) {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (proxy, m, args) -> switch (m.getName()) {
            case "isEnabled" -> enabled;
            case "getName" -> name;
            case "hashCode" -> name.hashCode();
            case "equals" -> proxy == args[0];
            default -> null;
        });
    }

    /** A guard plugin's break listener: says no, says yes or fails. Records each test event it's handed. */
    RegisteredListener guard(String plugin, String answer, List<String> seen) {
        RegisteredListener l = new RegisteredListener(new Listener() {}, (listener, event) -> {
            seen.add(plugin);
            if (answer.equals("fail")) throw new IllegalStateException("broken guard");
            if (answer.equals("no")) ((BlockBreakEvent) event).setCancelled(true);
        }, EventPriority.HIGHEST, named(plugin, true), false);
        BlockBreakEvent.getHandlerList().register(l);
        registered.add(l);
        return l;
    }

    final Block block = stub(Block.class, true);
    final Player player = stub(Player.class, true);

    @Test void aNamedGuardThatSaysNoStopsTheFall() {
        List<String> seen = new ArrayList<>();
        guard("Waycave", "no", seen);
        assertFalse(new Protection(null, log, List.of("Waycave")).mayBreak(player, block));
        assertEquals(List.of("Waycave"), seen);
    }

    @Test void aNamedGuardThatSaysYesLetsItFall() {
        List<String> seen = new ArrayList<>();
        guard("Waycave", "yes", seen);
        assertTrue(new Protection(null, log, List.of("waycave")).mayBreak(player, block), "names match in any case");
        assertEquals(List.of("Waycave"), seen);
    }

    @Test void pluginsNotNamedNeverSeeTheTestBreak() {
        List<String> seen = new ArrayList<>();
        guard("SomeLogger", "no", seen);
        assertTrue(new Protection(null, log, List.of("Waycave")).mayBreak(player, block));
        assertTrue(seen.isEmpty(), "only named plugins are asked, so loggers and skills never count a test break");
    }

    @Test void aNamedGuardThatFailsIsANo() {
        guard("Waycave", "fail", new ArrayList<>());
        assertFalse(new Protection(null, log, List.of("Waycave")).mayBreak(player, block));
    }

    @Test void withNoPlayerToAskANamedGuardMeansNo() {
        guard("Waycave", "yes", new ArrayList<>());
        assertFalse(new Protection(null, log, List.of("Waycave")).mayBreak(null, block));
    }

    @Test void aNamedPluginThatIsntInstalledIsSkipped() {
        assertTrue(new Protection(null, log, List.of("Waycave")).mayBreak(player, block));
        assertTrue(new Protection(null, log, List.of()).mayBreak(player, block));
    }

    @Test void griefPreventionStillDecidesFirst() {
        List<String> seen = new ArrayList<>();
        guard("Waycave", "yes", seen);
        assertFalse(new Protection(stub(Plugin.class, true), log, List.of("Waycave")).mayBreak(player, block));
        assertTrue(seen.isEmpty(), "a GriefPrevention no needs no second opinion");
    }

    @Test void aGuardThatSaysNoToBuildingStopsALanding() {
        RegisteredListener no = new RegisteredListener(new Listener() {}, (listener, event) -> ((BlockPlaceEvent) event).setCancelled(true),
            EventPriority.HIGHEST, named("Waycave", true), false);
        BlockPlaceEvent.getHandlerList().register(no);
        try {
            Protection p = new Protection(null, log, List.of("Waycave"));
            assertFalse(p.mayBuildAs(player, block), "a log never lands where the guard says no");
            assertFalse(p.mayBuildAs(null, block), "nobody to ask is a no");
        } finally {
            BlockPlaceEvent.getHandlerList().unregister(no);
        }
        assertTrue(new Protection(null, log, List.of("Waycave")).mayBuildAs(player, block), "with the guard gone, it lands");
    }
}
