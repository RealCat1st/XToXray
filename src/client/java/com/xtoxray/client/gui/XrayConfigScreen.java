package com.xtoxray.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.client.XrayToggleHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class XrayConfigScreen
extends Screen {
    private final Screen parent;
    private static final int CELL_SIZE = 22;
    private static final int CELL_GAP = 2;
    private int cols;
    private int gridLeft;
    private int gridRight;
    private int gridTop;
    private int sepY;
    private int doneY;
    private KeyMapping awaitingKeybindMapping;
    private final List<Button> keybindButtons = new ArrayList<>();
    private float gridScrollOffset = 0.0f;
    private float gridMaxScroll = 0.0f;

    public XrayConfigScreen(Screen parent) {
        super(Component.literal("X toXray Configuration"));
        this.parent = parent;
    }

    protected void init() {
        this.clearWidgets();
        this.keybindButtons.clear();
        XrayState xray = XrayState.getInstance();

        int rowH = 20;
        int gap = 4;

        int toggleBtnW = Math.min(180, (this.width - 80) / 2);
        int leftCol = (this.width - toggleBtnW * 2 - gap) / 2;
        int rightCol = leftCol + toggleBtnW + gap;

        int r = 20;

        this.addRenderableWidget(Button.builder(Component.literal("Vein Miner: " + (xray.isVeinMiner() ? "ON" : "OFF")), btn -> {
            xray.setVeinMiner(!xray.isVeinMiner());
            btn.setMessage(Component.literal("Vein Miner: " + (xray.isVeinMiner() ? "ON" : "OFF")));
        }).bounds(leftCol, r, toggleBtnW, rowH).build());

        this.addRenderableWidget(Button.builder(Component.literal("Show Hitboxes: " + (xray.isShowHitboxes() ? "ON" : "OFF")), btn -> {
            xray.setShowHitboxes(!xray.isShowHitboxes());
            btn.setMessage(Component.literal("Show Hitboxes: " + (xray.isShowHitboxes() ? "ON" : "OFF")));
        }).bounds(rightCol, r, toggleBtnW, rowH).build());

        r += rowH + gap;

        this.addRenderableWidget(Button.builder(Component.literal("Container View: " + (xray.isShowContainerView() ? "ON" : "OFF")), btn -> {
            xray.setShowContainerView(!xray.isShowContainerView());
            btn.setMessage(Component.literal("Container View: " + (xray.isShowContainerView() ? "ON" : "OFF")));
        }).bounds(leftCol, r, toggleBtnW, rowH).build());

        this.addRenderableWidget(Button.builder(Component.literal("Show Caves: " + (xray.isShowCaves() ? "ON" : "OFF")), btn -> {
            xray.setShowCaves(!xray.isShowCaves());
            btn.setMessage(Component.literal("Show Caves: " + (xray.isShowCaves() ? "ON" : "OFF")));
        }).bounds(rightCol, r, toggleBtnW, rowH).build());

        r += rowH + gap;

        this.addRenderableWidget(new RadiusSlider(30, r, this.width - 60, rowH));
        r += rowH + gap;

        int keybindSectionY = r;
        int keybindBtnW = Math.min(300, this.width - 60);

        for (KeybindEntry entry : KEYBINDS) {
            Button btn = Button.builder(keybindLabel(entry), b -> {
                this.awaitingKeybindMapping = entry.mapping();
                b.setMessage(Component.literal("Press a key..."));
            }).bounds((this.width - keybindBtnW) / 2, r, keybindBtnW, rowH).build();
            this.addRenderableWidget(btn);
            this.keybindButtons.add(btn);
            r += rowH + gap;
        }

        r += gap;
        r = Math.max(r, keybindSectionY + 30);
        this.sepY = r;
        this.doneY = this.height - 28;

        ArrayList<Block> whitelist = new ArrayList<Block>(xray.getWhitelist());
        this.cols = Math.max(5, (this.width - 40) / (CELL_SIZE + CELL_GAP));
        int gridW = this.cols * CELL_SIZE + (this.cols - 1) * CELL_GAP;
        this.gridLeft = (this.width - gridW) / 2;
        this.gridTop = this.sepY + 4;

        int cellStep = CELL_SIZE + CELL_GAP;
        int totalSlots = whitelist.size() + 1;
        int totalRows = (totalSlots + this.cols - 1) / this.cols;
        int totalGridH = totalRows * cellStep;
        int visibleH = this.doneY - 4 - this.gridTop;
        this.gridMaxScroll = Math.max(0, totalGridH - visibleH);
        this.gridScrollOffset = Math.max(0.0f, Math.min(this.gridScrollOffset, this.gridMaxScroll));

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
            .bounds(30, this.doneY, this.width - 60, rowH).build());
    }

    private void refreshKeybindButtons() {
        for (int i = 0; i < this.keybindButtons.size() && i < KEYBINDS.size(); i++) {
            this.keybindButtons.get(i).setMessage(keybindLabel(KEYBINDS.get(i)));
        }
    }

    private static Component keybindLabel(KeybindEntry entry) {
        String keyName = entry.mapping().getTranslatedKeyMessage().getString();
        return Component.literal(entry.label() + ": [" + keyName + "]");
    }

    private static final List<KeybindEntry> KEYBINDS = List.of(
        new KeybindEntry("Xray Toggle", XrayToggleHandler.TOGGLE_KEY),
        new KeybindEntry("Toggle Hitboxes", XrayToggleHandler.HITBOXES_KEY),
        new KeybindEntry("Toggle Caves", XrayToggleHandler.CAVES_KEY),
        new KeybindEntry("Container View", XrayToggleHandler.CONTAINER_VIEW_KEY),
        new KeybindEntry("Vein Miner", XrayToggleHandler.VEIN_MINER_KEY),
        new KeybindEntry("Open Settings", XrayToggleHandler.SETTINGS_KEY)
    );

    private record KeybindEntry(String label, KeyMapping mapping) {}

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, Component.literal("X toXray Configuration"), this.width / 2, 12, -1);
        int sepColor = -11184811;
        graphics.fill(20, this.sepY, this.width - 20, this.sepY + 1, sepColor);

        XrayState xray = XrayState.getInstance();
        ArrayList<Block> whitelist = new ArrayList<Block>(xray.getWhitelist());
        int totalSlots = whitelist.size() + 1;
        int cellStep = CELL_SIZE + CELL_GAP;

        for (int i = 0; i < totalSlots; i++) {
            int col = i % this.cols;
            int row = i / this.cols;
            int y = this.gridTop + row * cellStep - (int)this.gridScrollOffset;
            if (y + CELL_SIZE < this.gridTop || y > this.doneY - 4) continue;
            int x = this.gridLeft + col * cellStep;
            if (i < whitelist.size()) {
                renderCell(graphics, x, y, whitelist.get(i), mouseX, mouseY);
            } else {
                renderAddCell(graphics, x, y, mouseX, mouseY);
            }
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 || event.button() == 1) {
            XrayState xray = XrayState.getInstance();
            ArrayList<Block> whitelist = new ArrayList<Block>(xray.getWhitelist());
            int totalSlots = whitelist.size() + 1;
            int cellStep = CELL_SIZE + CELL_GAP;
            for (int i = 0; i < totalSlots; i++) {
                int col = i % this.cols;
                int row = i / this.cols;
                int x = this.gridLeft + col * cellStep;
                int y = this.gridTop + row * cellStep - (int)this.gridScrollOffset;
                if (event.x() >= x && event.x() < x + CELL_SIZE && event.y() >= y && event.y() < y + CELL_SIZE) {
                    if (i < whitelist.size()) {
                        if (event.button() == 0) {
                            xray.toggleDisabled(whitelist.get(i));
                        } else {
                            Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, whitelist.get(i)));
                        }
                    } else {
                        Minecraft.getInstance().setScreen(new AddBlockScreen(this));
                    }
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean keyPressed(KeyEvent event) {
        if (this.awaitingKeybindMapping != null) {
            if (event.isEscape()) {
                this.awaitingKeybindMapping = null;
                this.refreshKeybindButtons();
                return true;
            }
            InputConstants.Key newKey = InputConstants.Type.KEYSYM.getOrCreate(event.key());
            this.awaitingKeybindMapping.setKey(newKey);
            KeyMapping.resetMapping();
            this.awaitingKeybindMapping = null;
            this.refreshKeybindButtons();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.gridMaxScroll > 0.0f) {
            this.gridScrollOffset = Math.max(0.0f, Math.min(this.gridScrollOffset - (float)verticalAmount * 20.0f, this.gridMaxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
            System.err.println("[XtoXray] makeStack failed for " + block + ": " + t);
            return ItemStack.EMPTY;
        }
    }

    private void renderCell(GuiGraphicsExtractor graphics, int x, int y, Block block, int mouseX, int mouseY) {
        boolean disabled = XrayState.getInstance().isBlockDisabled(block);
        boolean hovered = mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
        int bgColor = disabled ? -15132391 : -14983648;
        int borderColor = hovered ? -1 : (disabled ? -11184811 : -11751600);
        graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, bgColor);
        graphics.fill(x, y, x + CELL_SIZE, y + 1, borderColor);
        graphics.fill(x, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + CELL_SIZE, borderColor);
        graphics.fill(x + CELL_SIZE - 1, y, x + CELL_SIZE, y + CELL_SIZE, borderColor);
        int iconX = x + (CELL_SIZE - 16) / 2;
        int iconY = y + (CELL_SIZE - 16) / 2;
        ItemStack stack = makeStack(block);
        if (!stack.isEmpty()) {
            graphics.fakeItem(stack, iconX, iconY);
        } else {
            renderBlockSprite(graphics, block, iconX, iconY);
        }
        if (disabled) {
            graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, -1426063360);
        }
    }

    private void renderAddCell(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
        int bgColor = -15066578;
        int borderColor = hovered ? -1 : -10453621;
        int plusColor = hovered ? -1 : -5592406;
        graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, bgColor);
        graphics.fill(x, y, x + CELL_SIZE, y + 1, borderColor);
        graphics.fill(x, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + CELL_SIZE, borderColor);
        graphics.fill(x + CELL_SIZE - 1, y, x + CELL_SIZE, y + CELL_SIZE, borderColor);
        MutableComponent plus = Component.literal("+");
        int tw = this.font.width((FormattedText)plus);
        graphics.text(this.font, (Component)plus, x + (CELL_SIZE - tw) / 2, y + (CELL_SIZE - 9) / 2, plusColor);
    }

    private static void renderBlockSprite(GuiGraphicsExtractor graphics, Block block, int x, int y) {
        graphics.fill(x, y, x + 16, y + 16, -11513212);
    }

    private class RadiusSlider
    extends AbstractSliderButton {
        public RadiusSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0.5);
            int currentDist = XrayState.getInstance().getOreRenderDistance();
            this.value = this.clamp((double)(currentDist - 32) / 480.0);
            this.updateMessage();
        }

        private double clamp(double v) {
            return Math.min(1.0, Math.max(0.0, v));
        }

        protected void updateMessage() {
            int dist = 32 + (int)(this.value * 480.0);
            this.setMessage((Component)Component.literal((String)("X-Ray Radius: " + dist + " blocks")));
        }

        protected void applyValue() {
            int dist = 32 + (int)(this.value * 480.0);
            XrayState.getInstance().setOreRenderDistance(dist);
        }
    }
}
