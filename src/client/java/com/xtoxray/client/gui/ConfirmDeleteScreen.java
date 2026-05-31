/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.level.block.Block
 */
package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class ConfirmDeleteScreen
extends Screen {
    private final Screen parent;
    private final Block block;

    public ConfirmDeleteScreen(Screen parent, Block block) {
        super((Component)Component.literal((String)"Remove Block"));
        this.parent = parent;
        this.block = block;
    }

    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"No"), btn -> this.onClose()).bounds(cx - 100, this.height / 2 + 10, 90, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Yes"), btn -> {
            XrayState.getInstance().getWhitelist().remove(this.block);
            XrayState.getInstance().save();
            Minecraft.getInstance().setScreen(this.parent);
        }).bounds(cx + 10, this.height / 2 + 10, 90, 20).build());
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, (Component)Component.literal((String)"Remove this block from whitelist?"), this.width / 2, this.height / 2 - 20, -1);
        graphics.centeredText(this.font, (Component)this.block.getName(), this.width / 2, this.height / 2 - 4, -22016);
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }
}



