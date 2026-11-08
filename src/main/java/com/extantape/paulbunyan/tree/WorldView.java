package com.extantape.paulbunyan.tree;

/** Read-only access to blocks, so the tree logic runs the same on a server and in tests. */
public interface WorldView {
    Cell cell(Pos p);
}
