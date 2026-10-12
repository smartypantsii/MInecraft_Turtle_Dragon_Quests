package com.jbarnes.turtledragonquests.client;

public final class ClientWallet {
    private static long tdg;

    private ClientWallet() {}

    public static long tdg() {
        return tdg;
    }

    public static void set(long tdg) {
        ClientWallet.tdg = Math.max(0L, tdg);
    }
}
