package com.dalkco.rightclickplants.mixin;

import com.dalkco.rightclickplants.PendingPlantQueue;
import com.dalkco.rightclickplants.PlantHelper;
import com.dalkco.rightclickplants.RightClickPlantsConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class UseBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        // Обрабатываем только основную руку, чтобы не дублировать
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!RightClickPlantsConfig.get().enabled) return;

        // Если игрок держит костную муку —
        // не перехватываем: пусть ваниль обрабатывает useOn сама.
        // Это фиксит краш: BoneMealItem.useOn() ожидает PASS, а не SUCCESS.
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof BoneMealItem) return;

        ClientLevel level = (ClientLevel) player.level();
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (!PlantHelper.isMatureCrop(state)) return;

        // Отменяем ванильное взаимодействие
        cir.setReturnValue(InteractionResult.SUCCESS);
        cir.cancel();

        // Ломаем блок — пакет уходит на сервер
        Direction hitFace = hitResult.getDirection();
        MultiPlayerGameMode gameMode = (MultiPlayerGameMode) (Object) this;
        gameMode.startDestroyBlock(pos, hitFace);
        gameMode.destroyBlock(pos);

        // Определяем семя и планируем посадку
        if (!RightClickPlantsConfig.get().replant) return;
        Item seed = PlantHelper.getSeed(state);
        if (seed == null) return;

        BlockPos supportPos = PlantHelper.getSupportPos(pos, state);
        Direction placeFace = PlantHelper.getPlaceFace(state);
        PendingPlantQueue.schedule(pos, supportPos, placeFace, seed);
    }

    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void onPerformUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof BoneMealItem) {
            // Bypasses the client-side BoneMealItem.useOn call to prevent ClassCastException crashes.
            // The interaction packet is still sent to the server, which handles the actual growth.
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }
}

