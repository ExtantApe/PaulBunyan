package com.extantape.paulbunyan.tools;

import java.util.Locale;

/** The other felling tools (felling-plan.md, "Bucking and tools"). */
public enum Tool {
    SMALL_WEDGE("Small Felling Wedge", 16, 0, "Set it in a one-block trunk and the tree falls away from it. You get it back."),
    LARGE_WEDGE("Large Felling Wedge", 16, 0, "Set it in a 2x2 trunk and the tree falls away from it. You get it back."),
    CROSSCUT_SAW("Crosscut Saw", 1, 250, "Hold right-click on a fallen section to saw through it, or on a trunk to fell it. Two players on opposite sides, taking turns swinging, saw a whole trunk in seconds."),
    PEAVEY("Peavey", 1, 250, "Right-click a fallen section to roll it one block, sideways across its length.");

    public final String title;
    public final int stack;
    /** Uses, or 0 for a tool that doesn't wear. */
    public final int uses;
    public final String help;

    Tool(String title, int stack, int uses, String help) {
        this.title = title;
        this.stack = stack;
        this.uses = uses;
        this.help = help;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Tool byId(String id) {
        for (Tool t : values()) if (t.id().equals(id)) return t;
        return null;
    }

    public boolean wedge() {
        return this == SMALL_WEDGE || this == LARGE_WEDGE;
    }
}
