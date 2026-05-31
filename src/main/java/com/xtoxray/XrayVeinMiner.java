/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.xtoxray;

import com.xtoxray.XrayState;
import java.util.ArrayDeque;
import java.util.HashSet;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class XrayVeinMiner {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel)) {
                return;
            }
            ServerLevel serverLevel = (ServerLevel)world;
            if (!XrayState.getInstance().isVeinMiner()) {
                return;
            }
            if (!XrayState.getInstance().isWhitelisted(state.getBlock())) {
                return;
            }
            XrayVeinMiner.breakVein(serverLevel, (ServerPlayer)player, pos, state.getBlock());
        });
    }

    private static void breakVein(ServerLevel world, ServerPlayer player, BlockPos startPos, Block block) {
        int maxBlocks = 64;
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        queue.add(startPos);
        visited.add(startPos);
        int broken = 0;
        while (!queue.isEmpty() && broken < maxBlocks) {
            BlockPos current = (BlockPos)queue.poll();
            ++broken;
            for (BlockPos neighbor : XrayVeinMiner.getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);
                BlockState neighborState = world.getBlockState(neighbor);
                if (!neighborState.is(block)) continue;
                queue.add(neighbor);
            }
        }
        int durabilityCost = XrayState.getInstance().getVeinMinerDurability();
        for (BlockPos pos : visited) {
            if (pos.equals(startPos)) continue;
            if (broken > maxBlocks) break;
            BlockState state = world.getBlockState(pos);
            if (!state.is(block)) continue;
            world.destroyBlock(pos, true, (Entity)player);
            if (durabilityCost > 0) {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty() && held.isDamageableItem()) {
                    held.hurtAndBreak(durabilityCost, player, EquipmentSlot.MAINHAND);
                }
            }
            ++broken;
        }
    }

    private static BlockPos[] getNeighbors(BlockPos pos) {
        return new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()};
    }
}



