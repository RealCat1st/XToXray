package com.xtoxray.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.network.XrayPayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LevelHeightAccessor;

public class XrayToggleHandler {
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.xtoxray.toggle", InputConstants.Type.KEYSYM, 88, KeyMapping.Category.MISC);
    public static final KeyMapping HITBOXES_KEY = new KeyMapping("key.xtoxray.hitboxes", InputConstants.Type.KEYSYM, -1, KeyMapping.Category.MISC);
    public static final KeyMapping CAVES_KEY = new KeyMapping("key.xtoxray.caves", InputConstants.Type.KEYSYM, -1, KeyMapping.Category.MISC);
    public static final KeyMapping CONTAINER_VIEW_KEY = new KeyMapping("key.xtoxray.containerview", InputConstants.Type.KEYSYM, -1, KeyMapping.Category.MISC);
    public static final KeyMapping VEIN_MINER_KEY = new KeyMapping("key.xtoxray.veinminer", InputConstants.Type.KEYSYM, -1, KeyMapping.Category.MISC);
    public static final KeyMapping SETTINGS_KEY = new KeyMapping("key.xtoxray.settings", InputConstants.Type.KEYSYM, -1, KeyMapping.Category.MISC);

    private static final KeyMapping[] ALL_KEYS = {TOGGLE_KEY, HITBOXES_KEY, CAVES_KEY, CONTAINER_VIEW_KEY, VEIN_MINER_KEY, SETTINGS_KEY};
    private static SectionPos lastSectionPos;
    private static volatile boolean serverAllowsXray;

    public static KeyMapping getToggleKey() {
        return TOGGLE_KEY;
    }

    public static KeyMapping[] getAllKeyMappings() {
        return ALL_KEYS;
    }

    public static void setServerAllowsXray(boolean value) {
        serverAllowsXray = value;
    }

    public static void register() {
        for (KeyMapping km : ALL_KEYS) {
            KeyMappingHelper.registerKeyMapping(km);
        }
        ClientTickEvents.END_CLIENT_TICK.register(XrayToggleHandler::onTick);
    }

    private static void onTick(Minecraft client) {
        XrayState state = XrayState.getInstance();
        SectionPos current;

        while (HITBOXES_KEY.consumeClick()) {
            state.setShowHitboxes(!state.isShowHitboxes());
        }
        while (CAVES_KEY.consumeClick()) {
            state.setShowCaves(!state.isShowCaves());
            if (client.level != null && client.player != null) {
                int renderDist = client.options.getEffectiveRenderDistance();
                SectionPos center = SectionPos.of(client.player.blockPosition());
                markSectionsDirty(client);
            }
        }
        while (CONTAINER_VIEW_KEY.consumeClick()) {
            state.setShowContainerView(!state.isShowContainerView());
        }
        while (VEIN_MINER_KEY.consumeClick()) {
            state.setVeinMiner(!state.isVeinMiner());
            if (client.getConnection() != null) {
                ClientPlayNetworking.send(new XrayPayloads.SyncVeinMinerC2S(state.isVeinMiner()));
            }
        }
        while (SETTINGS_KEY.consumeClick()) {
            client.setScreen(new com.xtoxray.client.gui.XrayConfigScreen(client.screen));
        }

        while (TOGGLE_KEY.consumeClick()) {
            if (client.level == null || client.player == null) continue;
            if (client.getCurrentServer() != null) {
                if (!serverAllowsXray) {
                    client.player.sendSystemMessage(Component.literal("\u00a7cX To Xray disables X-Ray on unsupported multiplayer servers by default to comply with Modrinth content rules.\nServer owners can enable support by installing the server-side component."));
                    continue;
                } else {
                    client.player.sendSystemMessage(Component.literal("\u00a7aThis Server Allows Xray!"));
                    state.toggle();
                }
            } else {
                state.toggle();
                if (state.isActive()) {
                    client.player.sendSystemMessage(Component.literal("§aTo Use Freecam Press F"));
                }
            }
            if (state.isActive()) {
                client.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
                lastSectionPos = SectionPos.of(client.player.blockPosition());
            } else {
                client.player.removeEffect(MobEffects.NIGHT_VISION);
                lastSectionPos = null;
            }
            rebuildAllSections(client);
        }

        if (state.isActive() && client.level != null && client.player != null && !(current = SectionPos.of(client.player.blockPosition())).equals(lastSectionPos)) {
            lastSectionPos = current;
            rebuildOreZone(client);
        }
    }

    private static void markSectionsDirty(Minecraft client) {
        if (client.levelRenderer == null || client.level == null || client.player == null) return;

        int viewDist = client.options.getEffectiveRenderDistance();
        LevelHeightAccessor heightAccessor = client.level;
        int minSY = heightAccessor.getMinSectionY();
        int maxSY = heightAccessor.getMaxSectionY() - 1;
        SectionPos cameraPos = SectionPos.of(client.player.blockPosition());
        int cx = cameraPos.getX();
        int cz = cameraPos.getZ();
        int minBX = SectionPos.sectionToBlockCoord(cx - viewDist);
        int minBY = SectionPos.sectionToBlockCoord(minSY);
        int minBZ = SectionPos.sectionToBlockCoord(cz - viewDist);
        int maxBX = SectionPos.sectionToBlockCoord(cx + viewDist + 1) - 1;
        int maxBY = SectionPos.sectionToBlockCoord(maxSY + 1) - 1;
        int maxBZ = SectionPos.sectionToBlockCoord(cz + viewDist + 1) - 1;

        if (triggerSodiumRebuild(client, minBX, minBY, minBZ, maxBX, maxBY, maxBZ)) {
            return;
        }

        client.levelRenderer.setBlocksDirty(minBX, minBY, minBZ, maxBX, maxBY, maxBZ);
    }

    private static boolean triggerSodiumRebuild(Minecraft client, int minBX, int minBY, int minBZ, int maxBX, int maxBY, int maxBZ) {
        if (!FabricLoader.getInstance().isModLoaded("sodium")) return false;
        try {
            Class<?> swrClass = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
            Object swr = swrClass.getMethod("instanceNullable").invoke(null);
            if (swr == null) {
                System.out.println("[XtoXray] SodiumWorldRenderer.instanceNullable() returned null");
                return false;
            }
            swrClass.getMethod("scheduleRebuildForBlockArea", int.class, int.class, int.class, int.class, int.class, int.class, boolean.class)
                .invoke(swr, minBX, minBY, minBZ, maxBX, maxBY, maxBZ, true);
            System.out.println("[XtoXray] Scheduled Sodium rebuild with important=true");
            return true;
        } catch (Exception e) {
            System.out.println("[XtoXray] triggerSodiumRebuild failed: " + e);
            return false;
        }
    }

    private static void rebuildAllSections(Minecraft client) {
        markSectionsDirty(client);
    }

    private static void rebuildOreZone(Minecraft client) {
        markSectionsDirty(client);
    }
}
