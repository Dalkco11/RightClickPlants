package com.dalkco.rightclickplants;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

public class PlantHelper {

    private static final Map<Block, Item> SEEDS = new HashMap<>();

    static {
        SEEDS.put(Blocks.WHEAT, Items.WHEAT_SEEDS);
        SEEDS.put(Blocks.CARROTS, Items.CARROT);
        SEEDS.put(Blocks.POTATOES, Items.POTATO);
        SEEDS.put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
        SEEDS.put(Blocks.NETHER_WART, Items.NETHER_WART);
        SEEDS.put(Blocks.COCOA, Items.COCOA_BEANS);
    }

    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block == Blocks.NETHER_WART) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block == Blocks.COCOA) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        return false;
    }

    public static Item getSeed(BlockState state) {
        return SEEDS.get(state.getBlock());
    }

    // Блок на который нужно «кликнуть» чтобы посадить семя
    public static BlockPos getSupportPos(BlockPos cropPos, BlockState state) {
        if (state.getBlock() == Blocks.COCOA) {
            Direction facing = state.getValue(CocoaBlock.FACING);
            return cropPos.relative(facing);
        }
        return cropPos.below();
    }

    // Сторона блока-опоры, на которую будем кликать
    public static Direction getPlaceFace(BlockState state) {
        if (state.getBlock() == Blocks.COCOA) {
            Direction facing = state.getValue(CocoaBlock.FACING);
            return facing.getOpposite();
        }
        return Direction.UP;
    }
}
