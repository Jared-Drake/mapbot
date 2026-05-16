package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PlacementScanner {

    private static final int SCAN_RADIUS = 5;

    public static Optional<PlacementTarget> findNearbyTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return Optional.empty();
        }

        BlockPos playerPos = mc.player.blockPosition();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {

                    BlockPos blockPos = playerPos.offset(x, y, z);
                    BlockState blockState = mc.level.getBlockState(blockPos);

                    if (blockState.isAir()) continue;

                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        BlockPos frontPos = blockPos.relative(face);

                        if (!mc.level.getBlockState(frontPos).isAir()) continue;

                        Vec3 eyePos = mc.player.getEyePosition();
                        Vec3 hitPos = Vec3.atCenterOf(blockPos).relative(face, 0.5);

                        BlockHitResult ray = mc.level.clip(new ClipContext(
                                eyePos,
                                hitPos,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                mc.player
                        ));

                        if (!ray.getBlockPos().equals(blockPos)) continue;
                        if (ray.getDirection() != face) continue;
                        BlockState frontState = mc.level.getBlockState(frontPos);

                        if (!frontState.isAir()) continue;

                        double distance = mc.player.distanceToSqr(
                                frontPos.getX() + 0.5,
                                frontPos.getY() + 0.5,
                                frontPos.getZ() + 0.5
                        );

                        if (distance > 25) continue;

                        if (UsedPlacementTracker.isUsed(frontPos)) {
                            continue;
                        }

                        if (tooCloseToUsed(frontPos)) {
                            continue;
                        }

                        return Optional.of(new PlacementTarget(blockPos, face));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean tooCloseToUsed(BlockPos pos) {
        for (BlockPos used : UsedPlacementTracker.getUsedPositions()) {
            if (used.distSqr(pos) < 4) {
                return true;
            }
        }

        return false;
    }
}