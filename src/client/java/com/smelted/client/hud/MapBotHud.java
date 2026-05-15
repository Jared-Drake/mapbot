package com.smelted.client.hud;

import com.smelted.client.bot.MapBotController;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class MapBotHud {

    public static void register() {

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {

            Minecraft mc = Minecraft.getInstance();

            if (mc.player == null) return;

            String status = "MapBot: " + MapBotController.getStatus();

            guiGraphics.drawString(
                    mc.font,
                    status,
                    10,
                    10,
                    0xFFFFFF,
                    true
            );
        });
    }
}