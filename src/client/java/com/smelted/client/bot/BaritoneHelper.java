package com.smelted.client.bot;

import net.minecraft.client.Minecraft;

public class BaritoneHelper {

    public static void goTo(int x, int z) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        mc.player.connection.sendChat("#goto " + x + " " + z);
    }

    public static void goTo(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        mc.player.connection.sendChat("#goto " + x + " " + y + " " + z);
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        mc.player.connection.sendChat("#stop");
    }
}
