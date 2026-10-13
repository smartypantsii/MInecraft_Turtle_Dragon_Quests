package com.jbarnes.turtledragonquests.client;

import net.minecraft.client.Minecraft;

/** Client-only; loaded by name from {@link ClientPayloads} so dedicated servers never resolve {@code Screen}. */
public final class ClientQuestLog {
    private ClientQuestLog() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new QuestLogScreen());
    }
}
