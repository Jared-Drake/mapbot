package com.smelted.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.smelted.MapBot;
import com.smelted.client.bot.MapBotController;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.network.chat.Component;

public class MapBotCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {

        dispatcher.register(
                ClientCommandManager.literal("mapbot")

                        .then(ClientCommandManager.literal("start")
                                .executes(context -> {

                                    MapBotController.start();

                                    context.getSource().sendFeedback(
                                            Component.literal("§aMapBot started.")
                                    );

                                    MapBot.LOGGER.info("MapBot started");

                                    return 1;
                                }))

                        .then(ClientCommandManager.literal("stop")
                                .executes(context -> {

                                    MapBotController.stop();

                                    context.getSource().sendFeedback(
                                            Component.literal("§cMapBot stopped.")
                                    );

                                    MapBot.LOGGER.info("MapBot stopped");

                                    return 1;
                                }))

                        .then(ClientCommandManager.literal("status")
                                .executes(context -> {

                                    context.getSource().sendFeedback(
                                            Component.literal("§eMapBot status: "
                                                    + MapBotController.getStatus())
                                    );

                                    return 1;
                                }))
        );
    }
}