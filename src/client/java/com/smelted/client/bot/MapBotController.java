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

    public static void start() {

        UsedPlacementTracker.clear();

        running = true;
        state = MapBotState.SCANNING;
        status = "Scanning";
    }

    public static void stop() {
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

                    status = "Target found";

                    state = MapBotState.LOOKING;

                } else {
                    status = "No valid nearby wall found";
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
                    state = MapBotState.SCANNING;
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

                cooldownTicks = ACTION_COOLDOWN;

                state = MapBotState.SCANNING;
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