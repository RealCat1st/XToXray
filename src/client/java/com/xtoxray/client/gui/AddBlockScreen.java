/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 */
package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class AddBlockScreen
extends Screen {
    private static final int COLS = 8;
    private static final int CELL = 22;
    private static final int GAP = 2;
    private final Screen parent;
    private EditBox searchBox;
    private List<Block> filteredBlocks = new ArrayList<Block>();
    private List<Block> allBlocks = new ArrayList<Block>();
    private Set<Integer> selectedIndices = new HashSet<>();
    private int scrollOffset = 0;

    public AddBlockScreen(Screen parent) {
        super((Component)Component.literal((String)"Add Block"));
        this.parent = parent;
    }

    protected void init() {
        int cx = this.width / 2;
        this.searchBox = new EditBox(this.font, cx - 100, 30, 200, 18, (Component)Component.literal((String)"Search"));
        this.searchBox.setHint((Component)Component.literal((String)"Search blocks..."));
        this.searchBox.setResponder(s -> {
            this.filterBlocks((String)s);
            this.selectedIndices.clear();
            this.scrollOffset = 0;
        });
        this.addRenderableWidget(this.searchBox);
        this.loadAllBlocks();
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Cancel"), btn -> this.onClose()).bounds(cx - 100, this.height - 28, 90, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Confirm"), btn -> this.confirmSelection()).bounds(cx + 10, this.height - 28, 90, 20).build());
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, (Component)Component.literal((String)"Add Block"), this.width / 2, 12, -1);
        int startY = 56;
        int rows = (this.filteredBlocks.size() + 8 - 1) / 8;
        int gridW = 190;
        int gridX = (this.width - gridW) / 2;
        int totalH = rows * 24;
        int visibleRows = (this.height - startY - 60) / 24;
        int maxOffset = Math.max(0, rows - visibleRows);
        for (int i = 0; i < this.filteredBlocks.size(); ++i) {
            int bgColor;
            int row = i / 8;
            int col = i % 8;
            if (row < this.scrollOffset || row >= this.scrollOffset + visibleRows + 1) continue;
            int x = gridX + col * 24;
            int y = startY + (row - this.scrollOffset) * 24;
            boolean selected = this.selectedIndices.contains(i);
            boolean inWhitelist = XrayState.getInstance().isWhitelisted(this.filteredBlocks.get(i));
            int itemBgColor = inWhitelist ? -14983648 : (selected ? -13408615 : -13154481);
            int borderColor = selected ? -22016 : (inWhitelist ? -11751600 : -10453621);
            graphics.fill(x, y, x + 22, y + 22, itemBgColor);
            graphics.fill(x, y, x + 22, y + 1, borderColor);
            graphics.fill(x, y + 22 - 1, x + 22, y + 22, borderColor);
            graphics.fill(x, y, x + 1, y + 22, borderColor);
            graphics.fill(x + 22 - 1, y, x + 22, y + 22, borderColor);
            Block block = this.filteredBlocks.get(i);
            ItemStack stack = makeStack(block);
            if (!stack.isEmpty()) {
                graphics.fakeItem(stack, x + 3, y + 3);
            }
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int startY = 56;
            int rows = (this.filteredBlocks.size() + 8 - 1) / 8;
            int gridW = 190;
            int gridX = (this.width - gridW) / 2;
            int visibleRows = (this.height - startY - 60) / 24;
            for (int i = 0; i < this.filteredBlocks.size(); ++i) {
                int row = i / 8;
                int col = i % 8;
                if (row < this.scrollOffset || row >= this.scrollOffset + visibleRows + 1) continue;
                int x = gridX + col * 24;
                int y = startY + (row - this.scrollOffset) * 24;
                if (!(event.x() >= (double)x) || !(event.x() <= (double)(x + 22)) || !(event.y() >= (double)y) || !(event.y() <= (double)(y + 22))) continue;
                if (this.selectedIndices.contains(i)) {
                    this.selectedIndices.remove(i);
                } else {
                    this.selectedIndices.add(i);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int startY = 56;
        int rows = (this.filteredBlocks.size() + 8 - 1) / 8;
        int visibleRows = (this.height - startY - 60) / 24;
        int maxOffset = Math.max(0, rows - visibleRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset - (int)verticalAmount, maxOffset));
        return true;
    }

    private void confirmSelection() {
        XrayState state = XrayState.getInstance();
        boolean changed = false;
        for (int i : this.selectedIndices) {
            if (i < 0 || i >= this.filteredBlocks.size()) continue;
            Block block = this.filteredBlocks.get(i);
            if (!state.isWhitelisted(block)) {
                state.getWhitelist().add(block);
                changed = true;
            }
        }
        if (changed) {
            state.save();
        }
        this.onClose();
    }

    private void loadAllBlocks() {
        this.allBlocks.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id;
            if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR || block.defaultBlockState().isAir() || (id = BuiltInRegistries.BLOCK.getKey(block)).getPath().contains("wall")) continue;
            this.allBlocks.add(block);
        }
        this.filterBlocks("");
    }

    private void filterBlocks(String query) {
        this.filteredBlocks.clear();
        String q = query.toLowerCase().trim();
        for (Block block : this.allBlocks) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            String name = id.getPath().replace('_', ' ');
            if (!q.isEmpty() && !id.getPath().contains(q) && !name.contains(q)) continue;
            this.filteredBlocks.add(block);
        }
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private static ItemStack makeStack(Block block) {
        if (block == Blocks.WATER) return new ItemStack((ItemLike)Items.WATER_BUCKET);
        if (block == Blocks.LAVA) return new ItemStack((ItemLike)Items.LAVA_BUCKET);
        try {
            Item item = block.asItem();
            java.util.Optional<Holder<Item>> opt = BuiltInRegistries.ITEM.getResourceKey(item)
                .flatMap(k -> BuiltInRegistries.ITEM.get(k.identifier()));
            if (opt.isPresent() && opt.get().areComponentsBound()) {
                return new ItemStack(opt.get(), 1);
            }
            Holder<Item> intrusiveHolder = item.builtInRegistryHolder();
            if (intrusiveHolder.areComponentsBound()) {
                return new ItemStack(intrusiveHolder, 1);
            }
            return ItemStack.EMPTY;
        } catch (Throwable t) {
            System.err.println("[XtoXray] AddBlockScreen makeStack failed for " + block + ": " + t);
            return ItemStack.EMPTY;
        }
    }
}



