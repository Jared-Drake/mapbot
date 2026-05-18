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

    private static final int STEP_DISTANCE = 20;
    private static final int MAX_INSERT_ATTEMPTS = 12;

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
                currentWaypoint = origin.offset(step * STEP_DISTANCE, 0, 0);
                step++;

                status = "Pathing to " + currentWaypoint.toShortString();

                BaritoneHelper.goTo(
                        currentWaypoint.getX(),
                        currentWaypoint.getZ()
                );

                cooldownTicks = 20;
                state = MapBotState.PATHING;
            }

            case PATHING -> {
                double dx = mc.player.getX() - currentWaypoint.getX();
                double dz = mc.player.getZ() - currentWaypoint.getZ();
                double distance = (dx * dx) + (dz * dz);

                status = "Pathing... " + Math.round(Math.sqrt(distance)) + " blocks away";

                if (distance <= 4) {
                    BaritoneHelper.stop();

                    status = "Close enough, placing";
                    cooldownTicks = 30;
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
                    cooldownTicks = 20;
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
                cooldownTicks = 40;
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
                    insertAttempts++;
                    status = "Waiting for frame entity... " + insertAttempts;
                    cooldownTicks = 10;
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
                cooldownTicks = 12;
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
                    cooldownTicks = 10;
                    state = MapBotState.WAITING_FOR_FRAME;
                    return;
                }

                RotationHelper.lookAtVec(mc, frame.getBoundingBox().getCenter());

                status = "Aiming at frame before insert";
                cooldownTicks = 8;
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
                    insertAttempts++;
                    status = "Waiting for frame entity... " + insertAttempts;
                    cooldownTicks = 10;
                    state = MapBotState.WAITING_FOR_FRAME;
                    return;
                }

                if (!frame.getItem().isEmpty()) {
                    status = "SUCCESS: map inserted, moving to next point";
                    UsedPlacementTracker.markUsed(frame.blockPosition());

                    insertAttempts = 0;
                    cooldownTicks = 20;
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                if (insertAttempts >= MAX_INSERT_ATTEMPTS) {
                    status = "Insert failed too many times; skipping target";
                    cooldownTicks = 20;
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                RotationHelper.lookAtVec(mc, frame.getBoundingBox().getCenter());

                if (!InteractionHelper.isCrosshairOnFrame(mc, frame)) {
                    status = "Crosshair not on frame, re-aiming";
                    cooldownTicks = 5;
                    state = MapBotState.WAITING_TO_INSERT_MAP;
                    return;
                }

                InteractionHelper.pressUseKey(mc);
                insertAttempts++;

                status = "Pressed use key attempt " + insertAttempts;

                cooldownTicks = 2;
                state = MapBotState.RELEASING_USE_KEY;
            }

            case RELEASING_USE_KEY -> {
                InteractionHelper.releaseUseKey(mc);

                status = "Released use key, checking frame contents";
                cooldownTicks = 20;
                state = MapBotState.WAITING_TO_INSERT_MAP;
            }
        }
    }

    public static String getStatus() {
        return status;
    }

    public static MapBotState getState() {
        return state;
    }

    public static boolean isRunning() {
        return running;
    }
}
