package com.dalkco.rightclickplants;

import com.dalkco.rightclickplants.mixin.InventoryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

    // Ограничиваем частоту показа action bar сообщения (раз в ~2 секунды)
    private static long lastHintTick = -100;
    private static final long HINT_COOLDOWN = 40;

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

            // Ищем семя только в хотбаре (слоты 0-8)
            int hotbarSlot = findSeedInHotbar(player, plant.seed());

            if (hotbarSlot != -1) {
                // Семя в хотбаре — сажаем
                if (tryPlant(client, player, plant, hotbarSlot)) {
                    iter.remove();
                }
            } else if (hasSeedInInventory(player, plant.seed())) {
                // Семя есть в инвентаре, но не в хотбаре — показываем подсказку
                showHint(client, player, plant.seed());
            }
        }
    }

    /** Ищет семя только среди 9 слотов хотбара. */
    private static int findSeedInHotbar(LocalPlayer player, Item seed) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(seed)) return i;
        }
        return -1;
    }

    /** Проверяет, есть ли семя в основном инвентаре (слоты 9-35). */
    private static boolean hasSeedInInventory(LocalPlayer player, Item seed) {
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).is(seed)) return true;
        }
        return false;
    }

    /** Показывает подсказку в action bar с ограничением по частоте. */
    private static void showHint(Minecraft client, LocalPlayer player, Item seed) {
        if (tick - lastHintTick < HINT_COOLDOWN) return;
        lastHintTick = tick;

        String seedName = seed.getDefaultInstance().getHoverName().getString();
        Component hint = Component.literal(
            "§e[Right Click Plants] §fПереложите §a" + seedName + " §fв быстрый доступ (слоты 1–9)"
        );
        client.gui.setOverlayMessage(hint, false);
    }

    private static boolean tryPlant(Minecraft client, LocalPlayer player, PendingPlant plant, int seedSlot) {
        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        int prevSlot = inv.getSelectedSlot();

        // Переключаемся на слот с семенем в хотбаре
        inv.setSelectedSlot(seedSlot);

        Vec3 hitPos = Vec3.atCenterOf(plant.supportPos()).relative(plant.placeFace(), 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, plant.placeFace(), plant.supportPos(), false);

        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        inv.setSelectedSlot(prevSlot);

        return result != InteractionResult.PASS && result != InteractionResult.FAIL;
    }
}

