package com.extantape.paulbunyan.tree;

/** The scanner's answer: a tree to fell, or the reason it won't. */
/** {@code detail} names the block that decided it, when there is one. */
public record Scan(Tree tree, Reason skip, String detail) {
    public enum Reason {
        NOT_A_TREE_LOG("not a natural log"),
        TOUCHES_BUILD("touches a build"),
        NO_NATURAL_LEAVES("has no natural leaves, so it may have been built"),
        NOT_ON_NYLIUM("a fungus that doesn't stand on nylium"),
        TOO_BIG("bigger than the size cap");

        public final String text;

        Reason(String text) {
            this.text = text;
        }
    }

    public static Scan fell(Tree t) {
        return new Scan(t, null, null);
    }

    public static Scan skip(Reason r) {
        return new Scan(null, r, null);
    }

    public static Scan skip(Reason r, String detail) {
        return new Scan(null, r, detail);
    }

    public boolean fells() {
        return tree != null;
    }
}
