package com.extantape.paulbunyan.hook;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Records block breaks and places in Prism (4.4 and up), when it's installed, as the player's own, so they can be looked
 * up and rolled back like any other. Prism's API is reached by name through its own class loader, so this plugin
 * doesn't need Prism, and a Prism that doesn't match is reported once and then left alone. A logging failure never
 * stops what the player is doing.
 */
public final class PrismLog {
    static final String API = "org.prism_mc.prism.paper.api.PrismPaperApi";
    static final String ACTIVITY = "org.prism_mc.prism.paper.api.activities.PaperActivity";

    private final Object api;
    private final Object breakType, placeType;
    private final Method createBlockAction, builder, recordingService;
    private final Logger log;
    private boolean warned;

    public PrismLog(Logger log) {
        Object a = null, br = null, pl = null;
        Method create = null, build = null, rec = null;
        Plugin prism = Bukkit.getPluginManager().getPlugin("prism");
        if (prism != null && prism.isEnabled()) {
            try {
                ClassLoader cl = prism.getClass().getClassLoader();
                Class<?> apiClass = Class.forName(API, true, cl);
                RegisteredServiceProvider<?> reg = Bukkit.getServicesManager().getRegistration(apiClass);
                if (reg == null) throw new IllegalStateException("Prism registered no API service");
                a = reg.getProvider();
                Object registry = call(a, "actionTypeRegistry");
                br = ((Optional<?>) call(registry, "actionType", "block-break")).orElseThrow();
                pl = ((Optional<?>) call(registry, "actionType", "block-place")).orElseThrow();
                Object factory = call(a, "actionFactory");
                for (Class<?> type : publicTypes(factory.getClass())) for (Method m : type.getMethods()) {
                    if (create == null && m.getName().equals("createBlockAction") && m.getParameterCount() == 2
                            && m.getParameterTypes()[1].isAssignableFrom(BlockState.class)) create = m;
                }
                if (create == null) throw new NoSuchMethodException("createBlockAction(ActionType, BlockState)");
                build = Class.forName(ACTIVITY, true, cl).getMethod("builder");
                if (!java.lang.reflect.Modifier.isPublic(build.getDeclaringClass().getModifiers())) throw new NoSuchMethodException("PaperActivity.builder() isn't public");
                rec = publicMethod(a, "recordingService");
                log.info("Recording in Prism too.");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                log.warning("Prism is installed but its API couldn't be reached (" + e + "). Nothing will show in Prism from here.");
                a = null;
            }
        }
        this.log = log;
        this.api = a;
        this.breakType = br;
        this.placeType = pl;
        this.createBlockAction = create;
        this.builder = build;
        this.recordingService = rec;
    }

    public boolean active() {
        return api != null;
    }

    /** Call before the block changes, so its state is what was broken. */
    public void broke(String who, Location at) {
        record(breakType, who, at);
    }

    /** Call after the block is placed, so its state is what was placed. */
    public void placed(String who, Location at) {
        record(placeType, who, at);
    }

    private void record(Object type, String who, Location at) {
        if (api == null) return;
        try {
            Object factory = call(api, "actionFactory");
            Object action = createBlockAction.invoke(factory, type, at.getBlock().getState());
            Object b = builder.invoke(null);
            b = call(b, "action", action);
            b = call(b, "location", at);
            Player p = Bukkit.getPlayerExact(who);
            b = p != null ? call(b, "cause", p) : call(b, "cause", who);
            Object activity = call(b, "build");
            Object service = recordingService.invoke(api);
            call(service, "addToQueue", activity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // A logging failure never stops the action. It's reported once, so a mismatch doesn't go unnoticed.
            if (!warned) {
                warned = true;
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warning("Couldn't record in Prism (" + cause + "). Felled blocks won't show in Prism until this is fixed.");
            }
        }
    }

    /** Calls a one-argument method by name, whichever of its overloads takes this argument. */
    static Object call(Object target, String name, Object arg) throws ReflectiveOperationException {
        for (Class<?> type : publicTypes(target.getClass())) {
            for (Method m : type.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(arg)) {
                    return m.invoke(target, arg);
                }
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + "(" + arg.getClass().getSimpleName() + ")");
    }

    /** Calls a method with no arguments, through a public type, since Prism's builders are hidden classes. */
    static Object call(Object target, String name) throws ReflectiveOperationException {
        for (Class<?> type : publicTypes(target.getClass())) {
            try {
                return type.getMethod(name).invoke(target);
            } catch (NoSuchMethodException next) {
                // try the next public type
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + "()");
    }

    /** A no-argument method looked up through a public type. */
    static Method publicMethod(Object target, String name) throws NoSuchMethodException {
        for (Class<?> type : publicTypes(target.getClass())) {
            try {
                return type.getMethod(name);
            } catch (NoSuchMethodException next) {
                // try the next public type
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + "()");
    }

    /**
     * The public types an object can be reached through: its own class if that's public, then its public superclasses
     * and interfaces. A public method of a hidden class can only be invoked through one of these.
     */
    static java.util.List<Class<?>> publicTypes(Class<?> c) {
        java.util.LinkedHashSet<Class<?>> out = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<Class<?>> todo = new java.util.ArrayDeque<>();
        todo.add(c);
        while (!todo.isEmpty()) {
            Class<?> k = todo.poll();
            if (java.lang.reflect.Modifier.isPublic(k.getModifiers())
                    && (k.getEnclosingClass() == null || java.lang.reflect.Modifier.isPublic(k.getEnclosingClass().getModifiers()))) out.add(k);
            if (k.getSuperclass() != null) todo.add(k.getSuperclass());
            todo.addAll(java.util.List.of(k.getInterfaces()));
        }
        return java.util.List.copyOf(out);
    }
}
