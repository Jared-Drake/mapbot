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
    private static final double MAX_INTERACTION_RANGE = 2.75;

    public static Optional<PlacementTarget> findNearbyTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return Optional.empty();
        }

        BlockPos playerPos = mc.player.blockPosition();

        PlacementTarget bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {

                    BlockPos blockPos = playerPos.offset(x, y, z);
                    BlockState blockState = mc.level.getBlockState(blockPos);

                    if (blockState.isAir()) continue;

                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        Optional<PlacementTarget> wallTarget =
                                validateTarget(mc, blockPos, face);
                        if (wallTarget.isPresent()) {
                            double distance = mc.player.getEyePosition()
                                    .distanceTo(Vec3.atCenterOf(wallTarget.get().framePos()));
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestTarget = wallTarget.get();
                            }
                        }
                    }

                    Optional<PlacementTarget> groundTarget =
                            validateTarget(mc, blockPos, Direction.UP);
                    if (groundTarget.isPresent()) {
                        double distance = mc.player.getEyePosition()
                                .distanceTo(Vec3.atCenterOf(groundTarget.get().framePos()));
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestTarget = groundTarget.get();
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(bestTarget);
    }

    private static boolean tooCloseToUsed(BlockPos pos) {
        for (BlockPos used : UsedPlacementTracker.getUsedPositions()) {
            if (used.distSqr(pos) < 4) {
                return true;
            }
        }

        return false;
    }

    private static Optional<PlacementTarget> validateTarget(
            Minecraft mc,
            BlockPos blockPos,
            Direction face
    ) {
        BlockPos framePos = blockPos.relative(face);

        if (!mc.level.getBlockState(framePos).isAir()) {
            return Optional.empty();
        }

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 hitPos = Vec3.atCenterOf(blockPos).relative(face, 0.5);
        Vec3 frameCenter = Vec3.atCenterOf(framePos);

        if (eyePos.distanceTo(frameCenter) > MAX_INTERACTION_RANGE) {
            return Optional.empty();
        }

        BlockHitResult ray = mc.level.clip(new ClipContext(
                eyePos,
                hitPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (!ray.getBlockPos().equals(blockPos)) {
            return Optional.empty();
        }

        if (ray.getDirection() != face) {
            return Optional.empty();
        }

        if (UsedPlacementTracker.isUsed(framePos) || tooCloseToUsed(framePos)) {
            return Optional.empty();
        }

        return Optional.of(new PlacementTarget(blockPos, face));
    }
}
