package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class MapBotController {

    private static boolean running = false;
    private static MapBotState state = MapBotState.STOPPED;
    private static String status = "Stopped";

    private static BlockPos origin;
    private static BlockPos currentWaypoint;
    private static int step = 1;

    private static int cooldownTicks = 0;

    private static final int STEP_DISTANCE = 20;

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

        status = "Started frame placement test";
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

                if (distance <= 25) {
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
                    status = "No wall found, moving on";
                    cooldownTicks = 20;
                    state = MapBotState.NEXT_POINT;
                    return;
                }

                BlockPos wallBlock = target.get().blockPos();
                Direction face = target.get().face();

                RotationHelper.lookAt(mc, wallBlock);

                InteractionHelper.placeItemFrame(mc, wallBlock, face);

                status = "Placed frame, moving on";
                cooldownTicks = 30;
                state = MapBotState.NEXT_POINT;
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