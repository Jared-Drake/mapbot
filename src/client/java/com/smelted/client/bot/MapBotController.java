package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public class MapBotController {

    private static boolean running = false;
    private static MapBotState state = MapBotState.STOPPED;
    private static String status = "Stopped";
    private static PlacementTarget currentTarget;
    private static int cooldownTicks = 0;
    private static final int ACTION_COOLDOWN = 10;
    private static int wanderTicks = 0;
    private static double lastX = 0;
    private static double lastZ = 0;
    private static int stuckTicks = 0;

    public static void start() {

        UsedPlacementTracker.clear();

        running = true;
        state = MapBotState.WANDERING;
        status = "Scanning";
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();

        MovementHelper.stopMovement(mc);

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

            case SCANNING -> {

                if (!InventoryHelper.hasItemFrames(mc)) {
                    status = "Out of item frames";
                    state = MapBotState.ERROR;
                    return;
                }

                if (!InventoryHelper.hasFilledMaps(mc)) {
                    status = "Out of filled maps";
                    state = MapBotState.ERROR;
                    return;
                }

                Optional<PlacementTarget> target =
                        PlacementScanner.findNearbyTarget(mc);

                if (target.isPresent()) {

                    currentTarget = target.get();
                    MovementHelper.stopMovement(mc);

                    status = "Target found";

                    state = MapBotState.LOOKING;

                } else {
                    status = "No valid nearby wall found";
                    state = MapBotState.WANDERING;
                }
            }

            case LOOKING -> {

                RotationHelper.lookAt(
                        mc,
                        currentTarget.blockPos()
                );

                status = "Looking at target";

                state = MapBotState.PLACING_FRAME;
            }

            case PLACING_FRAME -> {

                boolean selected =
                        InteractionHelper.selectItemFrame(mc);

                if (!selected) {
                    status = "No item frame in hotbar";
                    state = MapBotState.ERROR;
                    return;
                }

                InteractionHelper.placeItemFrame(
                        mc,
                        currentTarget.blockPos(),
                        currentTarget.face()
                );

                status = "Placed item frame";

                cooldownTicks = ACTION_COOLDOWN;

                state = MapBotState.INSERTING_MAP;

            }

            case WANDERING -> {

                status = "Wandering... Used: " + UsedPlacementTracker.size();

                MovementHelper.walkForward(mc);

                wanderTicks++;

                if (wanderTicks % 20 == 0) {
                    double dx = mc.player.getX() - lastX;
                    double dz = mc.player.getZ() - lastZ;
                    double moved = Math.sqrt(dx * dx + dz * dz);

                    if (moved < 0.2) {
                        stuckTicks++;
                    } else {
                        stuckTicks = 0;
                    }

                    lastX = mc.player.getX();
                    lastZ = mc.player.getZ();

                    if (stuckTicks >= 2) {
                        MovementHelper.randomTurn(mc);
                        stuckTicks = 0;
                        status = "Stuck, turning";
                    }
                }

                if (wanderTicks % 40 == 0) {
                    state = MapBotState.SCANNING;
                }
                if (wanderTicks % 80 == 0) {
                    MovementHelper.randomTurn(mc);
                }
            }

            case INSERTING_MAP -> {

                boolean selected =
                        InteractionHelper.selectFilledMap(mc);

                if (!selected) {
                    status = "No filled map in hotbar";
                    state = MapBotState.ERROR;
                    return;
                }

                var frame =
                        InteractionHelper.findNearbyFrame(
                                mc,
                                currentTarget.blockPos(),
                                currentTarget.face()
                        );

                if (frame == null) {
                    status = "Could not find item frame";
                    state = MapBotState.WANDERING;
                    return;
                }

                InteractionHelper.interactWithFrame(
                        mc,
                        frame
                );

                status = "Inserted map";

                UsedPlacementTracker.markUsed(
                        currentTarget.blockPos().relative(currentTarget.face())
                );

                MovementHelper.randomTurn(mc);
                wanderTicks = 0;

                cooldownTicks = ACTION_COOLDOWN;

                state = MapBotState.WANDERING;
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