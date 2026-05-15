package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class RotationHelper {

    public static void lookAt(Minecraft mc, BlockPos pos) {

        if (mc.player == null) return;

        double dx = pos.getX() + 0.5 - mc.player.getX();
        double dy = pos.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = pos.getZ() + 0.5 - mc.player.getZ();

        double distance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }
}