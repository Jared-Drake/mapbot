package com.smelted.client.bot;

import net.minecraft.client.Minecraft;

import java.util.Random;

public class MovementHelper {

    private static final Random RANDOM = new Random();

    public static void walkForward(Minecraft mc) {
        if (mc.options == null) return;

        mc.options.keyUp.setDown(true);
    }

    public static void stopMovement(Minecraft mc) {
        if (mc.options == null) return;

        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
    }

    public static void randomTurn(Minecraft mc) {
        if (mc.player == null) return;

        float yawChange = RANDOM.nextBoolean() ? 60F : -60F;
        mc.player.setYRot(mc.player.getYRot() + yawChange);
    }
}