package com.smelted.client.hud;

import com.smelted.client.bot.MapBotController;
import com.smelted.client.bot.MapBotState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

public class MapBotHud {

    private static final int COLOR_LABEL = 0xD0D0D0;
    private static final int COLOR_VALUE = 0xFFFFFF;
    private static final int COLOR_ON = 0x55FF55;
    private static final int COLOR_OFF = 0xFF5555;
    private static final int COLOR_WARNING = 0xFFFF55;
    private static final int COLOR_ERROR = 0xFF5555;

    public static void register() {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            int x = 10;
            int y = 10;
            int line = 10;

            boolean running = MapBotController.isRunning();
            MapBotState state = MapBotController.getState();

            drawLine(guiGraphics, mc, x, y, "MapBot:", running ? "ON" : "OFF", running ? COLOR_ON : COLOR_OFF);
            y += line;

            int stateColor = state == MapBotState.ERROR ? COLOR_ERROR
                    : state == MapBotState.PATHING ? COLOR_WARNING
                    : COLOR_VALUE;
            drawLine(guiGraphics, mc, x, y, "State:", state.name(), stateColor);
            y += line;

            int statusColor = state == MapBotState.ERROR ? COLOR_ERROR : COLOR_VALUE;
            drawLine(guiGraphics, mc, x, y, "Status:", MapBotController.getStatus(), statusColor);
            y += line;

            BlockPos waypoint = MapBotController.getCurrentWaypoint();
            drawLine(guiGraphics, mc, x, y, "Waypoint:", waypoint == null ? "-" : waypoint.toShortString(), COLOR_VALUE);
            y += line;

            double distance = MapBotController.getDistanceToWaypoint(mc);
            drawLine(guiGraphics, mc, x, y, "Distance:", distance < 0 ? "-" : String.format("%.1f", distance), COLOR_VALUE);
            y += line;

            drawLine(guiGraphics, mc, x, y, "Maps placed:", String.valueOf(MapBotController.getMapsPlaced()), COLOR_ON);
            y += line;

            drawLine(guiGraphics, mc, x, y, "Failed/skipped:", String.valueOf(MapBotController.getFailedTargets()), COLOR_WARNING);
            y += line;

            drawLine(guiGraphics, mc, x, y, "Stuck skips:", String.valueOf(MapBotController.getStuckSkips()), COLOR_WARNING);
            y += line;

            drawLine(guiGraphics, mc, x, y, "Insert attempts:", String.valueOf(MapBotController.getInsertAttempts()), COLOR_VALUE);
        });
    }

    private static void drawLine(GuiGraphics guiGraphics, Minecraft mc, int x, int y, String label, String value, int valueColor) {
        guiGraphics.drawString(mc.font, label, x, y, COLOR_LABEL, true);
        guiGraphics.drawString(mc.font, value, x + 90, y, valueColor, true);
    }
}
