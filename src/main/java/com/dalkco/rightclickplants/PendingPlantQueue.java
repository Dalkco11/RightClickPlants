package com.dalkco.rightclickplants;

import com.dalkco.rightclickplants.mixin.InventoryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PendingPlantQueue {

    private record PendingPlant(BlockPos cropPos, BlockPos supportPos, Direction placeFace, Item seed, long expiryTick) {}

    private static final List<PendingPlant> queue = new ArrayList<>();
    private static long tick = 0;

    public static void schedule(BlockPos cropPos, BlockPos supportPos, Direction placeFace, Item seed) {
        queue.removeIf(p -> p.cropPos().equals(cropPos));
        queue.add(new PendingPlant(cropPos, supportPos, placeFace, seed, tick + 100)); // 5 сек
    }

    public static void tick(Minecraft client) {
        tick++;
        if (queue.isEmpty()) return;

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) return;

        Iterator<PendingPlant> iter = queue.iterator();
        while (iter.hasNext()) {
            PendingPlant plant = iter.next();

            if (tick > plant.expiryTick()) {
                iter.remove();
                continue;
            }

            int slot = findSeedSlot(player, plant.seed());
            if (slot == -1) continue;

            if (tryPlant(client, player, plant, slot)) {
                iter.remove();
            }
        }
    }

    private static int findSeedSlot(LocalPlayer player, Item seed) {
        // Сначала хотбар
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(seed)) return i;
        }
        // Потом основной инвентарь
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).is(seed)) return i;
        }
        return -1;
    }

    private static boolean tryPlant(Minecraft client, LocalPlayer player, PendingPlant plant, int seedSlot) {
        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        int prevSlot = inv.getSelectedSlot();

        if (seedSlot < 9) {
            inv.setSelectedSlot(seedSlot);
        } else {
            // Переносим предмет из инвентаря в текущий слот хотбара
            player.getInventory().pickSlot(seedSlot);
        }

        Vec3 hitPos = Vec3.atCenterOf(plant.supportPos()).relative(plant.placeFace(), 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, plant.placeFace(), plant.supportPos(), false);

        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        inv.setSelectedSlot(prevSlot);

        return result != InteractionResult.PASS && result != InteractionResult.FAIL;
    }
}
