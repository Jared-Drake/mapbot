package com.smelted.client;

import com.smelted.MapBot;
import com.smelted.client.command.MapBotCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.smelted.client.hud.MapBotHud;
import com.smelted.client.bot.MapBotController;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MapBotClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        MapBot.LOGGER.info("MapBot client initialized");
        MapBotHud.register();
        ClientTickEvents.END_CLIENT_TICK.register(MapBotController::tick);

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {
                    MapBotCommands.register(dispatcher);
                }
        );
    }
}