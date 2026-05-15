package com.smelted.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryHelper {

    public static boolean hasItemFrames(Minecraft mc) {
        return countItem(mc, Items.ITEM_FRAME) > 0;
    }

    public static boolean hasFilledMaps(Minecraft mc) {
        return countItem(mc, Items.FILLED_MAP) > 0;
    }

    public static int countItemFrames(Minecraft mc) {
        return countItem(mc, Items.ITEM_FRAME);
    }

    public static int countFilledMaps(Minecraft mc) {
        return countItem(mc, Items.FILLED_MAP);
    }

    private static int countItem(Minecraft mc, Item item) {
        if (mc.player == null) return 0;

        int count = 0;

        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }
}