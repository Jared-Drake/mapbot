package com.smelted.client.bot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record PlacementTarget(BlockPos blockPos, Direction face) {
}