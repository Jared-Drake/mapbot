package com.smelted.client.bot;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class UsedPlacementTracker {

    private static final Set<BlockPos> USED_POSITIONS =
            new HashSet<>();

    public static boolean isUsed(BlockPos pos) {
        return USED_POSITIONS.contains(pos);
    }

    public static void markUsed(BlockPos pos) {
        USED_POSITIONS.add(pos);
    }

    public static void clear() {
        USED_POSITIONS.clear();
    }

    public static int size() {
        return USED_POSITIONS.size();
    }
    public static Set<BlockPos> getUsedPositions() {
        return USED_POSITIONS;
    }
}