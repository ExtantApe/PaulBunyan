package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.tree.Species;
import java.util.Locale;

/** The handle woods: every overworld and nether wood that grows as a tree PaulBunyan fells. */
public enum Handle {
    OAK(Species.OAK, "STRIPPED_OAK_LOG"),
    SPRUCE(Species.SPRUCE, "STRIPPED_SPRUCE_LOG"),
    BIRCH(Species.BIRCH, "STRIPPED_BIRCH_LOG"),
    JUNGLE(Species.JUNGLE, "STRIPPED_JUNGLE_LOG"),
    ACACIA(Species.ACACIA, "STRIPPED_ACACIA_LOG"),
    DARK_OAK(Species.DARK_OAK, "STRIPPED_DARK_OAK_LOG"),
    MANGROVE(Species.MANGROVE, "STRIPPED_MANGROVE_LOG"),
    CHERRY(Species.CHERRY, "STRIPPED_CHERRY_LOG"),
    PALE_OAK(Species.PALE_OAK, "STRIPPED_PALE_OAK_LOG"),
    CRIMSON(Species.CRIMSON, "STRIPPED_CRIMSON_STEM"),
    WARPED(Species.WARPED, "STRIPPED_WARPED_STEM");

    /** The trees of this wood, where the axe swings faster and bucks five logs. */
    public final Species species;
    /** The handle in the recipe, as a material name. */
    public final String material;

    Handle(Species species, String material) {
        this.species = species;
        this.material = material;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** "Dark oak", for the item's description. */
    public String title() {
        String s = id().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static Handle byId(String id) {
        for (Handle h : values()) if (h.id().equals(id)) return h;
        return null;
    }
}
