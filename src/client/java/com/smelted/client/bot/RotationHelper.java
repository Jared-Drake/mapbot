package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class RotationHelper {

    public static void lookAt(Minecraft mc, BlockPos pos) {
        lookAtVec(mc, Vec3.atCenterOf(pos));
    }

    public static void lookAtVec(Minecraft mc, Vec3 target) {
        if (mc.player == null) return;

        Vec3 eyePos = mc.player.getEyePosition();

        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }
}