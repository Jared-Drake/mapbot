package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PlacementScanner {

    private static final int SCAN_RADIUS = 7;
    private static final int VERTICAL_SCAN_MIN = -3;
    private static final int VERTICAL_SCAN_MAX = 3;
    private static final double MAX_INTERACTION_RANGE = 3.75;

    private static ScanStats lastScanStats = new ScanStats();

    public static Optional<PlacementTarget> findNearbyTarget(Minecraft mc, BlockPos scanCenter) {
        if (mc.player == null || mc.level == null) {
            lastScanStats = new ScanStats();
            return Optional.empty();
        }

        BlockPos playerPos = scanCenter != null ? scanCenter : mc.player.blockPosition();

        PlacementTarget bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        ScanStats stats = new ScanStats();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = VERTICAL_SCAN_MIN; y <= VERTICAL_SCAN_MAX; y++) {
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
            if (used.distSqr(pos) < 1.5) {
                return true;
            }
        }
        for (BlockPos failed : UsedPlacementTracker.getFailedPositions()) {
            if (failed.distSqr(pos) < 1.1) {
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

        if (ray.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && !ray.getDirection().equals(face)
                && !ray.getDirection().equals(face.getOpposite())) {
            // Keep practical tolerance: as long as ray hits the support block, allow minor face mismatch.
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
