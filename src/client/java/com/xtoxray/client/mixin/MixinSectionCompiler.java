/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.chunk.RenderSectionRegion
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={RenderSectionRegion.class})
public class MixinSectionCompiler {
    @Inject(method={"getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"}, at={@At(value="RETURN")}, cancellable=true)
    private void xtoxray$modifyBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = (BlockState)cir.getReturnValue();
        if (state == null) {
            return;
        }
        if (!XrayState.getInstance().isActive()) {
            return;
        }
        XrayState xray = XrayState.getInstance();
        if (state.isAir()) {
            if (xray.isShowCaves()) {
                cir.setReturnValue(Blocks.GOLD_BLOCK.defaultBlockState());
            }
            return;
        }
        if (!xray.shouldRender(state)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
            return;
        }
        int maxDist = xray.getOreRenderDistance();
        if (maxDist > 0 && !MixinSectionCompiler.isInRange(pos, maxDist)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }

    private static boolean isInRange(BlockPos pos, int maxDist) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return true;
        }
        return pos.distSqr((Vec3i)client.player.blockPosition()) < (double)maxDist * (double)maxDist;
    }
}



