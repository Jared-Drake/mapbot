package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class InteractionHelper {

    public static boolean selectItemFrame(Minecraft mc) {

        if (mc.player == null) return false;

        for (int i = 0; i < 9; i++) {

            if (mc.player.getInventory().getItem(i).getItem() == Items.ITEM_FRAME) {
                mc.player.getInventory().selected = i;
                return true;
            }
        }

        return false;
    }

    public static boolean selectFilledMap(Minecraft mc) {

        if (mc.player == null) return false;

        for (int i = 0; i < 9; i++) {

            if (mc.player.getInventory().getItem(i).getItem() == Items.FILLED_MAP) {
                mc.player.getInventory().selected = i;
                return true;
            }
        }

        return false;
    }

    public static ItemFrame findNearbyFrame(
            Minecraft mc,
            BlockPos blockPos,
            Direction face
    ) {
        if (mc.level == null) return null;

        BlockPos frameSpace = blockPos.relative(face);

        AABB searchBox = new AABB(frameSpace).inflate(1.0);

        for (ItemFrame frame : mc.level.getEntitiesOfClass(ItemFrame.class, searchBox)) {
            if (frame.getDirection() == face) {
                return frame;
            }
        }

        return null;
    }

    public static void interactWithFrame(
            Minecraft mc,
            ItemFrame frame
    ) {

        if (mc.gameMode == null || mc.player == null) return;

        mc.gameMode.interact(
                mc.player,
                frame,
                InteractionHand.MAIN_HAND
        );
    }

    public static void placeItemFrame(
            Minecraft mc,
            BlockPos blockPos,
            Direction face
    ) {

        if (mc.gameMode == null || mc.player == null) return;

        Vec3 hitVec = Vec3.atCenterOf(blockPos);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                face,
                blockPos,
                false
        );

        mc.gameMode.useItemOn(
                mc.player,
                InteractionHand.MAIN_HAND,
                hitResult
        );
    }
}