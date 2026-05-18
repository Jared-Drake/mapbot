package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PlacementScanner {

    private static final int SCAN_RADIUS = 5;
    private static final double MAX_INTERACTION_RANGE = 2.75;

    private static ScanStats lastScanStats = new ScanStats();

    public static Optional<PlacementTarget> findNearbyTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            lastScanStats = new ScanStats();
            return Optional.empty();
        }

        BlockPos playerPos = mc.player.blockPosition();

        PlacementTarget bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        ScanStats stats = new ScanStats();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {

                    BlockPos blockPos = playerPos.offset(x, y, z);
                    BlockState blockState = mc.level.getBlockState(blockPos);

                    if (blockState.isAir()) continue;
                    stats.scannedBlocks++;

                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        Optional<PlacementTarget> wallTarget = validateTarget(mc, blockPos, face, stats);
                        if (wallTarget.isPresent()) {
                            double distance = mc.player.getEyePosition()
                                    .distanceTo(Vec3.atCenterOf(wallTarget.get().framePos()));
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestTarget = wallTarget.get();
                            }
                        }
                    }

                    Optional<PlacementTarget> groundTarget = validateTarget(mc, blockPos, Direction.UP, stats);
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

        lastScanStats = stats;
        return Optional.ofNullable(bestTarget);
    }

    public static String getLastScanDebugSummary() {
        return "scan blocks=" + lastScanStats.scannedBlocks
                + " support=" + lastScanStats.rejectedSupport
                + " occupied=" + lastScanStats.rejectedOccupied
                + " range=" + lastScanStats.rejectedRange
                + " raycast=" + lastScanStats.rejectedRaycast;
    }

    private static boolean tooCloseToUsed(BlockPos pos) {
        for (BlockPos used : UsedPlacementTracker.getUsedPositions()) {
            if (used.distSqr(pos) < 4) {
                return true;
            }
        }
        for (BlockPos failed : UsedPlacementTracker.getFailedPositions()) {
            if (failed.distSqr(pos) < 2) {
                return true;
            }
        }

        return false;
    }

    private static Optional<PlacementTarget> validateTarget(
            Minecraft mc,
            BlockPos blockPos,
            Direction face,
            ScanStats stats
    ) {
        BlockPos framePos = blockPos.relative(face);
        BlockState supportState = mc.level.getBlockState(blockPos);

        if (!isValidSupportFace(mc, blockPos, supportState, face)) {
            stats.rejectedSupport++;
            return Optional.empty();
        }

        if (!mc.level.getBlockState(framePos).isAir()) {
            stats.rejectedOccupied++;
            return Optional.empty();
        }

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 hitPos = Vec3.atCenterOf(blockPos).relative(face, 0.5);
        Vec3 frameCenter = Vec3.atCenterOf(framePos);

        if (eyePos.distanceTo(frameCenter) > MAX_INTERACTION_RANGE) {
            stats.rejectedRange++;
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
            stats.rejectedRaycast++;
            return Optional.empty();
        }

        if (UsedPlacementTracker.isUsed(framePos)
                || UsedPlacementTracker.isFailed(framePos)
                || tooCloseToUsed(framePos)) {
            return Optional.empty();
        }

        return Optional.of(new PlacementTarget(blockPos, face));
    }

    private static boolean isValidSupportFace(
            Minecraft mc,
            BlockPos blockPos,
            BlockState supportState,
            Direction face
    ) {
        if (supportState.isAir()) return false;
        if (!supportState.getFluidState().isEmpty()) return false;
        if (supportState.canBeReplaced()) return false;
        if (!supportState.getCollisionShape(mc.level, blockPos).isEmpty()) {
            return supportState.isFaceSturdy(mc.level, blockPos, face, SupportType.FULL);
        }
        return false;
    }

    private static class ScanStats {
        int scannedBlocks = 0;
        int rejectedSupport = 0;
        int rejectedOccupied = 0;
        int rejectedRange = 0;
        int rejectedRaycast = 0;
    }
}
