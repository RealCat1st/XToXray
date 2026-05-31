package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice")
public class MixinSodiumLevelSlice {
    @Inject(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true, require = 0)
    private void xtoxray$modifyBlockState(int blockX, int blockY, int blockZ, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
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
        if (maxDist > 0 && !MixinSodiumLevelSlice.isInRange(blockX, blockY, blockZ, maxDist)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }

    private static boolean isInRange(int x, int y, int z, int maxDist) {
        double dz;
        double dy;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return true;
        }
        double dx = client.player.getX() - (double)x;
        return dx * dx + (dy = client.player.getY() - (double)y) * dy + (dz = client.player.getZ() - (double)z) * dz < (double)maxDist * (double)maxDist;
    }
}



