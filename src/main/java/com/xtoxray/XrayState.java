/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.xtoxray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xtoxray.XtoXray;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class XrayState {
    private static final XrayState INSTANCE = new XrayState();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private boolean active;
    private final Set<Block> whitelist = new HashSet<Block>();
    private final Set<Block> disabledBlocks = new HashSet<Block>();
    private boolean veinMiner;
    private int oreRenderDistance = 128;
    private boolean showHitboxes;
    private boolean showContainerView = true;
    private boolean showCaves;
    private String shownChangelogVersion = "";
    private boolean neverShowChangelog;
    private int veinMinerDurability = 1;

    private XrayState() {
    }

    public static XrayState getInstance() {
        return INSTANCE;
    }

    public void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("xtoxray.json");
        if (!Files.exists(path, new LinkOption[0])) {
            this.initDefaults();
            this.save();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path);){
            ConfigData data = (ConfigData)GSON.fromJson((Reader)reader, ConfigData.class);
            if (data == null) {
                this.initDefaults();
                this.save();
                return;
            }
            this.oreRenderDistance = data.oreRenderDistance;
            this.veinMiner = data.veinMiner;
            this.showHitboxes = data.showHitboxes;
            this.showContainerView = data.showContainerView;
            this.shownChangelogVersion = data.shownChangelogVersion != null ? data.shownChangelogVersion : "";
            this.neverShowChangelog = data.neverShowChangelog;
            this.veinMinerDurability = data.veinMinerDurability;
            this.whitelist.clear();
            for (String id : data.whitelist) {
                BuiltInRegistries.BLOCK.get(Identifier.parse((String)id)).map(holder -> (Block)holder.value()).ifPresent(this.whitelist::add);
            }
            if (this.whitelist.isEmpty()) {
                this.initDefaults();
            }
        }
        catch (Exception e) {
            XtoXray.LOGGER.error("Failed to load config", (Throwable)e);
            this.initDefaults();
        }
    }

    public void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("xtoxray.json");
        ConfigData data = new ConfigData();
        data.oreRenderDistance = this.oreRenderDistance;
        data.veinMiner = this.veinMiner;
        data.showHitboxes = this.showHitboxes;
        data.showContainerView = this.showContainerView;
        data.shownChangelogVersion = this.shownChangelogVersion;
        data.neverShowChangelog = this.neverShowChangelog;
        data.veinMinerDurability = this.veinMinerDurability;
        for (Block b : this.whitelist) {
            data.whitelist.add(BuiltInRegistries.BLOCK.getKey(b).toString());
        }
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(path, new OpenOption[0]);){
                GSON.toJson(data, (Appendable)writer);
            }
        }
        catch (IOException e) {
            XtoXray.LOGGER.error("Failed to save config", (Throwable)e);
        }
    }

private void initDefaults() {
        this.whitelist.add(Blocks.COAL_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_COAL_ORE);
        this.whitelist.add(Blocks.IRON_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_IRON_ORE);
        this.whitelist.add(Blocks.COPPER_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_COPPER_ORE);
        this.whitelist.add(Blocks.GOLD_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_GOLD_ORE);
        this.whitelist.add(Blocks.REDSTONE_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        this.whitelist.add(Blocks.EMERALD_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_EMERALD_ORE);
        this.whitelist.add(Blocks.LAPIS_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_LAPIS_ORE);
        this.whitelist.add(Blocks.DIAMOND_ORE);
        this.whitelist.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        this.whitelist.add(Blocks.NETHER_GOLD_ORE);
        this.whitelist.add(Blocks.NETHER_QUARTZ_ORE);
        this.whitelist.add(Blocks.ANCIENT_DEBRIS);
        this.whitelist.add(Blocks.CHEST);
        this.whitelist.add(Blocks.TRAPPED_CHEST);
        this.whitelist.add(Blocks.SPAWNER);
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void toggle() {
        this.active = !this.active;
    }

    public boolean shouldRender(BlockState state) {
        return !this.active || this.whitelist.contains(state.getBlock()) && !this.disabledBlocks.contains(state.getBlock());
    }

    public Set<Block> getWhitelist() {
        return this.whitelist;
    }

    public boolean isWhitelisted(Block block) {
        return this.whitelist.contains(block);
    }

    public boolean isBlockDisabled(Block block) {
        return this.disabledBlocks.contains(block);
    }

    public void toggleDisabled(Block block) {
        if (this.disabledBlocks.contains(block)) {
            this.disabledBlocks.remove(block);
        } else {
            this.disabledBlocks.add(block);
        }
        this.save();
    }

    public boolean isVeinMiner() {
        return this.veinMiner;
    }

    public void setVeinMiner(boolean veinMiner) {
        this.veinMiner = veinMiner;
        this.save();
    }

    public int getOreRenderDistance() {
        return this.oreRenderDistance;
    }

    public void setOreRenderDistance(int blocks) {
        this.oreRenderDistance = blocks;
        this.save();
    }

    public boolean isShowHitboxes() {
        return this.showHitboxes;
    }

    public void setShowHitboxes(boolean showHitboxes) {
        this.showHitboxes = showHitboxes;
        this.save();
    }

    public boolean isShowContainerView() {
        return this.showContainerView;
    }

    public void setShowContainerView(boolean showContainerView) {
        this.showContainerView = showContainerView;
        this.save();
    }

    public boolean isShowCaves() {
        return this.showCaves;
    }

    public void setShowCaves(boolean showCaves) {
        this.showCaves = showCaves;
        this.save();
    }

    public String getShownChangelogVersion() {
        return this.shownChangelogVersion;
    }

    public void setShownChangelogVersion(String version) {
        this.shownChangelogVersion = version;
        this.save();
    }

    public boolean isNeverShowChangelog() {
        return this.neverShowChangelog;
    }

    public void setNeverShowChangelog(boolean value) {
        this.neverShowChangelog = value;
        this.save();
    }

    public int getVeinMinerDurability() {
        return this.veinMinerDurability;
    }

    public void setVeinMinerDurability(int amount) {
        this.veinMinerDurability = Math.max(0, amount);
        this.save();
    }

    private static class ConfigData {
        int oreRenderDistance = 128;
        boolean veinMiner;
        boolean showHitboxes;
        boolean showContainerView = true;
        String shownChangelogVersion = "";
        boolean neverShowChangelog;
        int veinMinerDurability = 1;
        List<String> whitelist = new ArrayList<String>();

        private ConfigData() {
        }
    }
}



