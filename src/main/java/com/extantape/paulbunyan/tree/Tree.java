package com.extantape.paulbunyan.tree;

import java.util.List;

/**
 * A tree the scanner is sure of: the natural logs that fall, the leaves that go with them, and the
 * logs at the bottom, where the replant goes. {@code strays} are placed logs touching it, which stay.
 */
public record Tree(Species species, List<Pos> logs, List<Pos> leaves, List<Pos> base, int minY, int strays) {}
