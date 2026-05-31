package com.xtoxray.client;

import com.xtoxray.XrayState;
import com.xtoxray.client.gui.ChangelogScreen;
import com.xtoxray.network.XrayPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public class XtoXrayClient implements ClientModInitializer {
    private static boolean shownThisSession = false;

    public void onInitializeClient() {
        XrayState.getInstance().load();
        XrayToggleHandler.register();
        ContainerViewHandler.register();

        ClientPlayNetworking.registerGlobalReceiver(XrayPayloads.HandshakeS2C.TYPE, (payload, context) -> {
            XrayToggleHandler.setServerAllowsXray(true);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sender.sendPacket(new XrayPayloads.HandshakeC2S());
            sender.sendPacket(new XrayPayloads.SyncVeinMinerC2S(XrayState.getInstance().isVeinMiner()));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            XrayToggleHandler.setServerAllowsXray(false);
            XrayState.getInstance().setActive(false);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen && !shownThisSession && !XrayState.getInstance().isNeverShowChangelog()) {
                shownThisSession = true;
                client.setScreen(new ChangelogScreen(screen));
            }
        });
    }
}
