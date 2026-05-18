package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;

public class MapBotController {

    private static boolean running = false;
    private static MapBotState state = MapBotState.STOPPED;
    private static String status = "Stopped";

    private static BlockPos origin;
    private static BlockPos currentWaypoint;
    private static PlacementTarget lastPlacementTarget;

    private static int step = 1;
    private static int cooldownTicks = 0;
    private static int insertAttempts = 0;
    private static int frameWaitAttempts = 0;
    private static int successCount = 0;
    private static int skippedCount = 0;
    private static int stuckSkips = 0;

    private static Vec3 pathingStartPosition;
    private static int pathingStuckTicks = 0;

    private static final int STEP_DISTANCE = 20;
    private static final int MIN_WAYPOINT_Y = 64;
    private static final int MAX_INSERT_ATTEMPTS = 12;
    private static final int COOLDOWN_AFTER_GOTO = 12;
    private static final int COOLDOWN_AFTER_PATH_REACHED = 12;
    private static final int COOLDOWN_AFTER_PLACE = 12;
    private static final int COOLDOWN_WAIT_FRAME_RETRY = 6;
    private static final int COOLDOWN_AFTER_MAP_SELECT = 6;
    private static final int COOLDOWN_REAIM = 4;
    private static final int COOLDOWN_BEFORE_INSERT = 4;
    private static final int COOLDOWN_USE_RELEASE = 1;
    private static final int COOLDOWN_AFTER_RELEASE = 8;
    private static final int COOLDOWN_SKIP = 10;
    private static final int STUCK_TIMEOUT_TICKS = 100;
    private static final double STUCK_MOVE_THRESHOLD = 0.3;

    public static void start() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            status = "Player or world missing";
            state = MapBotState.ERROR;
            return;
        }

        running = true;
        origin = mc.player.blockPosition();
        step = 1;
        insertAttempts = 0;
        frameWaitAttempts = 0;
        successCount = 0;
        skippedCount = 0;
        stuckSkips = 0;
        currentWaypoint = null;
        lastPlacementTarget = null;
        resetPathingStuckTimer();

        status = "Started map insert test";
        state = MapBotState.NEXT_POINT;
    }

    public static void stop() {
        BaritoneHelper.stop();

        running = false;
        state = MapBotState.STOPPED;
        status = "Stopped";
    }

    public static void tick(Minecraft mc) {
        if (!running || mc.player == null || mc.level == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        switch (state) {

            case NEXT_POINT -> {
                int waypointY = Math.max(origin.getY(), MIN_WAYPOINT_Y);
                currentWaypoint = new BlockPos(
                        origin.getX() + (step * STEP_DISTANCE),
                        waypointY,
                        origin.getZ()
                );
                step++;
                resetPathingStuckTimer();

                status = "Pathing to " + currentWaypoint.toShortString();

                BaritoneHelper.goTo(
                        currentWaypoint.getX(),
                        currentWaypoint.getY(),
                        currentWaypoint.getZ()
                );

                cooldownTicks = COOLDOWN_AFTER_GOTO;
                state = MapBotState.PATHING;
            }

            case PATHING -> {
                if (currentWaypoint == null) {
                    status = "Waypoint missing, selecting next point";
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                updatePathingStuckTracker(mc);

                double dx = mc.player.getX() - currentWaypoint.getX();
                double dy = mc.player.getY() - currentWaypoint.getY();
                double dz = mc.player.getZ() - currentWaypoint.getZ();
                double distanceSqr = (dx * dx) + (dy * dy) + (dz * dz);

                status = "Pathing... " + Math.round(Math.sqrt(distanceSqr)) + " blocks away";

                if (pathingStuckTicks >= STUCK_TIMEOUT_TICKS) {
                    BaritoneHelper.stop();
                    skippedCount++;
                    stuckSkips++;
                    status = "Baritone stuck, skipping to next point";
                    cooldownTicks = COOLDOWN_SKIP;
                    resetPathingStuckTimer();
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                if (distanceSqr <= 4) {
                    BaritoneHelper.stop();
                    resetPathingStuckTimer();

                    status = "Close enough, placing";
                    cooldownTicks = COOLDOWN_AFTER_PATH_REACHED;
                    state = MapBotState.PLACING_FRAME;
                }
            }

            case PLACING_FRAME -> {
                boolean selected = InteractionHelper.selectItemFrame(mc);

                if (!selected) {
                    status = "No item frames in hotbar";
                    state = MapBotState.ERROR;
                    return;
                }

                var target = PlacementScanner.findNearbyTarget(mc);

                if (target.isEmpty()) {
                    status = "No reachable wall/ground target, moving on";
                    skippedCount++;
                    cooldownTicks = COOLDOWN_SKIP;
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                lastPlacementTarget = target.get();

                RotationHelper.lookAtVec(
                        mc,
                        Vec3.atCenterOf(lastPlacementTarget.blockPos())
                                .relative(lastPlacementTarget.face(), 0.5)
                );

                InteractionHelper.placeItemFrame(
                        mc,
                        lastPlacementTarget.blockPos(),
                        lastPlacementTarget.face()
                );

                status = "Placed frame, testing map insert";
                insertAttempts = 0;
                frameWaitAttempts = 0;
                cooldownTicks = COOLDOWN_AFTER_PLACE;
                state = MapBotState.WAITING_FOR_FRAME;
            }

            case WAITING_FOR_FRAME -> {
                if (lastPlacementTarget == null) {
                    status = "No placement target selected; skipping";
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                ItemFrame frame = InteractionHelper.findNearbyFrame(
                        mc,
                        lastPlacementTarget.blockPos(),
                        lastPlacementTarget.face()
                );

                if (frame == null) {
                    frameWaitAttempts++;
                    if (frameWaitAttempts >= 8) {
                        UsedPlacementTracker.markFailed(lastPlacementTarget.framePos());
                        skippedCount++;
                        status = withStats("Frame failed to appear, skipping target");
                        cooldownTicks = COOLDOWN_SKIP;
                        state = MapBotState.NEXT_POINT;
                        return;
                    }
                    status = withStats("Waiting for frame entity... " + frameWaitAttempts);
                    cooldownTicks = COOLDOWN_WAIT_FRAME_RETRY;
                    return;
                }

                boolean selected = InteractionHelper.selectFilledMap(mc);

                if (!selected) {
                    status = "No filled maps in hotbar";
                    state = MapBotState.ERROR;
                    return;
                }

                RotationHelper.lookAtVec(mc, frame.getBoundingBox().getCenter());

                status = "Map selected, waiting before insert";
                cooldownTicks = COOLDOWN_AFTER_MAP_SELECT;
                state = MapBotState.WAITING_TO_INSERT_MAP;
            }

            case WAITING_TO_INSERT_MAP -> {
                if (lastPlacementTarget == null) {
                    status = "No placement target selected; skipping";
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                ItemFrame frame = InteractionHelper.findNearbyFrame(
                        mc,
                        lastPlacementTarget.blockPos(),
                        lastPlacementTarget.face()
                );

                if (frame == null) {
                    status = "Frame disappeared, retrying";
                    cooldownTicks = COOLDOWN_WAIT_FRAME_RETRY;
                    state = MapBotState.WAITING_FOR_FRAME;
                    return;
                }

                RotationHelper.lookAtVec(mc, frame.getBoundingBox().getCenter());

                status = withStats("Aiming at frame before insert");
                cooldownTicks = COOLDOWN_BEFORE_INSERT;
                state = MapBotState.INSERTING_MAP_TEST;
            }

            case INSERTING_MAP_TEST -> {
                if (lastPlacementTarget == null) {
                    status = "No placement target selected; skipping";
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                ItemFrame frame = InteractionHelper.findNearbyFrame(
                        mc,
                        lastPlacementTarget.blockPos(),
                        lastPlacementTarget.face()
                );

                if (frame == null) {
                    frameWaitAttempts++;
                    if (frameWaitAttempts >= 8) {
                        UsedPlacementTracker.markFailed(lastPlacementTarget.framePos());
                        skippedCount++;
                        status = withStats("Frame disappeared too long, skipping target");
                        cooldownTicks = COOLDOWN_SKIP;
                        state = MapBotState.NEXT_POINT;
                        return;
                    }
                    status = withStats("Waiting for frame entity... " + frameWaitAttempts);
                    cooldownTicks = COOLDOWN_WAIT_FRAME_RETRY;
                    state = MapBotState.WAITING_FOR_FRAME;
                    return;
                }

                if (!frame.getItem().isEmpty()) {
                    successCount++;
                    status = withStats("SUCCESS: map inserted, moving to next point");
                    UsedPlacementTracker.markUsed(frame.blockPosition());

                    insertAttempts = 0;
                    frameWaitAttempts = 0;
                    cooldownTicks = COOLDOWN_SKIP;
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                if (insertAttempts >= MAX_INSERT_ATTEMPTS) {
                    UsedPlacementTracker.markFailed(lastPlacementTarget.framePos());
                    skippedCount++;
                    status = withStats("Insert failed too many times; skipping target");
                    cooldownTicks = COOLDOWN_SKIP;
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                RotationHelper.lookAtVec(mc, frame.getBoundingBox().getCenter());

                if (!InteractionHelper.isCrosshairOnFrame(mc, frame)) {
                    status = withStats("Crosshair not on frame, re-aiming");
                    cooldownTicks = COOLDOWN_REAIM;
                    state = MapBotState.WAITING_TO_INSERT_MAP;
                    return;
                }

                InteractionHelper.pressUseKey(mc);
                insertAttempts++;

                status = withStats("Pressed use key attempt " + insertAttempts);

                cooldownTicks = COOLDOWN_USE_RELEASE;
                state = MapBotState.RELEASING_USE_KEY;
            }

            case RELEASING_USE_KEY -> {
                InteractionHelper.releaseUseKey(mc);

                status = withStats("Released use key, checking frame contents");
                cooldownTicks = COOLDOWN_AFTER_RELEASE;
                state = MapBotState.WAITING_TO_INSERT_MAP;
            }

            case STOPPED, ERROR -> {
                // No tick work in passive states.
            }
        }
    }

    private static void updatePathingStuckTracker(Minecraft mc) {
        Vec3 currentPos = mc.player.position();
        if (pathingStartPosition == null) {
            pathingStartPosition = currentPos;
            pathingStuckTicks = 0;
            return;
        }

        if (currentPos.distanceTo(pathingStartPosition) >= STUCK_MOVE_THRESHOLD) {
            pathingStartPosition = currentPos;
            pathingStuckTicks = 0;
            return;
        }

        pathingStuckTicks++;
    }

    private static void resetPathingStuckTimer() {
        pathingStartPosition = null;
        pathingStuckTicks = 0;
    }

    private static String withStats(String message) {
        return "[" + state + "] " + message
                + " | insert=" + insertAttempts
                + "/" + MAX_INSERT_ATTEMPTS
                + " frameWait=" + frameWaitAttempts
                + "/" + 8
                + " success=" + successCount
                + " skipped=" + skippedCount
                + " stuck=" + stuckSkips;
    }

    public static String getStatus() { return status; }
    public static MapBotState getState() { return state; }
    public static boolean isRunning() { return running; }
    public static BlockPos getCurrentWaypoint() { return currentWaypoint; }
    public static int getMapsPlaced() { return successCount; }
    public static int getFailedTargets() { return skippedCount; }
    public static int getStuckSkips() { return stuckSkips; }
    public static int getInsertAttempts() { return insertAttempts; }

    public static double getDistanceToWaypoint(Minecraft mc) {
        if (mc == null || mc.player == null || currentWaypoint == null) return -1;
        double dx = mc.player.getX() - currentWaypoint.getX();
        double dy = mc.player.getY() - currentWaypoint.getY();
        double dz = mc.player.getZ() - currentWaypoint.getZ();
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }
}
