package com.extantape.paulbunyan.hook;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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
}
