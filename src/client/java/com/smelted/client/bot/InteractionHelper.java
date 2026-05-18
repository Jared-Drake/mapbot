package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class InteractionHelper {

    public static boolean selectItemFrame(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.ITEM_FRAME) {
                mc.player.getInventory().selected = i;
                mc.player.connection.send(
                        new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(i)
                );
                return true;
            }
        }

        return false;
    }

    public static boolean selectFilledMap(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.FILLED_MAP) {
                mc.player.getInventory().selected = i;
                mc.player.connection.send(
                        new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(i)
                );
                return true;
            }
        }

        return false;
    }

    public static ItemFrame findNearbyFrame(Minecraft mc, BlockPos blockPos, Direction face) {
        if (mc.level == null) return null;

        BlockPos frameSpace = blockPos.relative(face);
        AABB searchBox = new AABB(frameSpace).inflate(0.75);

        ItemFrame best = null;
        double bestDistance = Double.MAX_VALUE;

        Vec3 expected = Vec3.atCenterOf(frameSpace);

        for (ItemFrame frame : mc.level.getEntitiesOfClass(ItemFrame.class, searchBox)) {
            double distance = frame.position().distanceToSqr(expected);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = frame;
            }
        }

        return best;
    }

    public static boolean interactWithFrame(Minecraft mc, ItemFrame frame) {
        if (mc.gameMode == null || mc.player == null || frame == null) {
            return false;
        }

        if (!mc.player.getMainHandItem().is(Items.FILLED_MAP)) {
            return false;
        }

        mc.gameMode.interact(
                mc.player,
                frame,
                InteractionHand.MAIN_HAND
        );

        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static void pressUseKey(Minecraft mc) {
        if (mc.options == null) return;
        mc.options.keyUse.setDown(true);
    }

    public static void releaseUseKey(Minecraft mc) {
        if (mc.options == null) return;
        mc.options.keyUse.setDown(false);
    }

    public static boolean isCrosshairOnFrame(Minecraft mc, ItemFrame frame) {
        if (mc.hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity() == frame;
        }

        return false;
    }

    public static void placeItemFrame(Minecraft mc, BlockPos blockPos, Direction face) {
        if (mc.gameMode == null || mc.player == null) return;

        Vec3 center = Vec3.atCenterOf(blockPos);
        Vec3 hitVec = face == Direction.UP
                ? center.add(0.0, 0.5, 0.0)
                : center.relative(face, 0.5);

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

        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}

