/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.Identifier
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.BarrelBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.ShulkerBoxBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 */
package com.xtoxray.client;

import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ContainerViewHandler {
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath((String)"xtoxray", (String)"container_view");
    private static volatile ContainerViewData currentView = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> ContainerViewHandler.tick(client));
        HudElementRegistry.attachElementBefore((Identifier)VanillaHudElements.MISC_OVERLAYS, (Identifier)ELEMENT_ID, ContainerViewHandler::render);
    }

    private static void tick(Minecraft client) {
        BlockHitResult blockHit;
        block12: {
            block11: {
                if (!XrayState.getInstance().isActive() || !XrayState.getInstance().isShowContainerView() || client.player == null || client.level == null) {
                    currentView = null;
                    return;
                }
                HitResult hit = client.hitResult;
                if (!(hit instanceof BlockHitResult)) break block11;
                blockHit = (BlockHitResult)hit;
                if (hit.getType() == HitResult.Type.BLOCK) break block12;
            }
            currentView = null;
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        Block block = client.level.getBlockState(pos).getBlock();
        if (!ContainerViewHandler.isContainerBlock(block)) {
            currentView = null;
            return;
        }
        if (currentView != null && ContainerViewHandler.currentView.pos.equals(pos)) {
            return;
        }
        if (client.hasSingleplayerServer()) {
            BlockPos immPos = pos.immutable();
            ResourceKey dimension = client.level.dimension();
            MutableComponent name = block.getName();
            client.getSingleplayerServer().executeIfPossible(() -> ContainerViewHandler.lambda$tick$0(client, dimension, immPos, (Component)name));
        } else {
            BlockEntity be = client.level.getBlockEntity(pos);
            if (be instanceof Container) {
                Container container = (Container)be;
                int size = container.getContainerSize();
                ArrayList<ItemStack> items = new ArrayList<ItemStack>(size);
                for (int i = 0; i < size; ++i) {
                    items.add(container.getItem(i).copy());
                }
                XtoXray.LOGGER.info("ContainerView: client container at {} size={}", pos, size);
                currentView = new ContainerViewData(pos, items, (Component)block.getName());
            } else {
                XtoXray.LOGGER.info("ContainerView: no container at {} from client", pos);
            }
        }
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (currentView == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int cols = 9;
        int slotSize = 18;
        int gap = 2;
        int rows = Math.max(1, (ContainerViewHandler.currentView.items.size() + cols - 1) / cols);
        int panelW = cols * (slotSize + gap) - gap + 20;
        int panelH = rows * (slotSize + gap) - gap + 50;
        int panelX = (screenWidth - panelW) / 2;
        int panelY = (screenHeight - panelH) / 2;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, -1072689136);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, -11751600);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, -11751600);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, -11751600);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, -11751600);
        graphics.centeredText(mc.font, ContainerViewHandler.currentView.name, screenWidth / 2, panelY + 8, -1);
        int startX = panelX + 10;
        int startY = panelY + 28;
        for (int i = 0; i < ContainerViewHandler.currentView.items.size(); ++i) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (slotSize + gap);
            int y = startY + row * (slotSize + gap);
            graphics.fill(x, y, x + slotSize, y + slotSize, -13421773);
            graphics.fill(x, y, x + slotSize, y + 1, -11184811);
            graphics.fill(x, y + slotSize - 1, x + slotSize, y + slotSize, -11184811);
            graphics.fill(x, y, x + 1, y + slotSize, -11184811);
            graphics.fill(x + slotSize - 1, y, x + slotSize, y + slotSize, -11184811);
            ItemStack stack = ContainerViewHandler.currentView.items.get(i);
            if (stack.isEmpty()) continue;
            graphics.fakeItem(stack, x + 1, y + 1);
            graphics.itemDecorations(mc.font, stack, x + 1, y + 1);
        }
    }

    private static boolean isContainerBlock(Block block) {
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock;
    }

    private static /* synthetic */ void lambda$tick$0(Minecraft client, ResourceKey dimension, BlockPos immPos, Component name) {
        ServerLevel serverLevel = client.getSingleplayerServer().getLevel(dimension);
        if (serverLevel != null) {
            BlockEntity be = serverLevel.getBlockEntity(immPos);
            if (be instanceof Container) {
                Container container = (Container)be;
                int size = container.getContainerSize();
                ArrayList<ItemStack> items = new ArrayList<ItemStack>(size);
                boolean hasItems = false;
                for (int i = 0; i < size; ++i) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        hasItems = true;
                    }
                    items.add(stack.copy());
                }
                XtoXray.LOGGER.info("ContainerView: server task got container {} size={} hasItems={}", new Object[]{immPos, size, hasItems});
                currentView = new ContainerViewData(immPos, items, name);
            } else {
                XtoXray.LOGGER.info("ContainerView: server BE at {} is not container: {}", immPos, be);
            }
        } else {
            XtoXray.LOGGER.info("ContainerView: serverLevel null for dimension {}", dimension);
        }
    }

    private record ContainerViewData(BlockPos pos, List<ItemStack> items, Component name) {
    }
}



