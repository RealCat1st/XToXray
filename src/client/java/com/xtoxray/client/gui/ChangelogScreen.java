/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonObject
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Util
 */
package com.xtoxray.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import com.xtoxray.client.gui.ChangelogDetailScreen;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class ChangelogScreen
extends Screen {
    private static final URI DISCORD_URI = URI.create("https://discord.gg/E5fkjKUgWJ");
    private static final URI MODRINTH_URI = URI.create("https://modrinth.com/mod/x-to-xray");
    private static final URI MODRINTH_API = URI.create("https://api.modrinth.com/v2/project/HbXXzLHU/version");
    static final String CURRENT_VERSION = "2026.15";
    private static final int ROW_H = 28;
    private static final int COL_GAP = 8;
    private static final int PAD = 24;
    private static final int CORNER = 4;
    private final Screen parent;
    private volatile String versionStatus = "Checking...";
    private volatile int versionColor = -10262799;
    private volatile List<VersionEntry> allVersions;
    private volatile int loadedCount = 0;
    private boolean showBeta = true;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int tableX;
    private int colWVersion;
    private int colWGames;
    private int colWChangelog;
    private int tableTop;
    private int tableAreaTop;
    private int tableAreaBottom;
    private int scrollbarX;
    private int scrollbarY;
    private int scrollbarH;
    private boolean draggingScroll;
    private float scrollOffset;
    private float maxScroll;
    private int hoveredRow = -1;
    private final List<ClickTarget> clickTargets = new ArrayList<ClickTarget>();
    private int btnAreaTop;
    private int btnAreaH;
    private int btnContinueX;
    private int btnContinueY;
    private int btnContinueW;
    private int btnContinueH;
    private int btnDiscordX;
    private int btnDiscordY;
    private int btnDiscordW;
    private int btnDiscordH;
    private int btnModrinthX;
    private int btnModrinthY;
    private int btnModrinthW;
    private int btnModrinthH;
    private int btnNeverX;
    private int btnNeverY;
    private int btnNeverW;
    private int btnNeverH;

    public ChangelogScreen(Screen parent) {
        super((Component)Component.literal((String)"X toXray"));
        this.parent = parent;
    }

    protected void init() {
        this.panelW = Math.min(this.width - 80, 720);
        int headerH = 108;
        int availH = this.height - 40;
        this.btnAreaH = Math.min(106, Math.max(56, availH / 4));
        int tableH = Math.max(60, availH - headerH - this.btnAreaH);
        this.panelH = Math.min(availH, headerH + tableH + this.btnAreaH);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = Math.max(20, (this.height - this.panelH) / 2);
        int btnCenterX = this.panelX + this.panelW / 2;
        this.btnAreaTop = this.panelY + this.panelH - this.btnAreaH;
        this.btnContinueX = btnCenterX - 100;
        this.btnContinueY = this.btnAreaTop + 8;
        this.btnContinueW = 200;
        this.btnContinueH = 26;
        this.btnDiscordX = btnCenterX - 100;
        this.btnDiscordY = this.btnAreaTop + 38;
        this.btnDiscordW = 96;
        this.btnDiscordH = 20;
        this.btnModrinthX = btnCenterX + 4;
        this.btnModrinthY = this.btnAreaTop + 38;
        this.btnModrinthW = 96;
        this.btnModrinthH = 20;
        this.btnNeverX = btnCenterX - 100;
        this.btnNeverY = this.btnAreaTop + 64;
        this.btnNeverW = 200;
        this.btnNeverH = 20;
        this.tableX = this.panelX + 24;
        int tableW = this.panelW - 48;
        this.colWVersion = Math.min(110, tableW / 4);
        this.colWGames = Math.min(130, tableW / 4);
        this.colWChangelog = tableW - this.colWVersion - this.colWGames - 16;
        this.tableAreaTop = this.tableTop = this.panelY + headerH;
        this.tableAreaBottom = this.btnAreaTop - 4;
        int scrollW = 12;
        this.scrollbarX = this.panelX + this.panelW - 24 - scrollW;
        this.scrollbarY = this.tableAreaTop;
        this.scrollbarH = this.tableAreaBottom - this.tableAreaTop;
        this.checkForUpdates();
    }

    private void checkForUpdates() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(MODRINTH_API).header("User-Agent", "xtoxray/" + XtoXray.VERSION).GET().build();
        ((CompletableFuture)client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            block10: {
                if (response.statusCode() == 200) {
                    try {
                        Gson gson = new Gson();
                        JsonArray arr = (JsonArray)gson.fromJson((String)response.body(), JsonArray.class);
                        ArrayList<VersionEntry> entries = new ArrayList<VersionEntry>();
                        String currentMcVersion = FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
                        String latestNumeric = null;
                        for (int i = 0; i < arr.size(); ++i) {
                            JsonObject obj = arr.get(i).getAsJsonObject();
                            String v = obj.get("version_number").getAsString();
                            String changelog = obj.has("changelog") && !obj.get("changelog").isJsonNull() ? obj.get("changelog").getAsString() : "";
                            JsonArray gameArr = obj.get("game_versions").getAsJsonArray();
                            ArrayList<String> gameVersions = new ArrayList<String>();
                            boolean matchesCurrentMc = false;
                            for (int j = 0; j < gameArr.size(); ++j) {
                                String gv = gameArr.get(j).getAsString();
                                if (gv.equals(currentMcVersion)) matchesCurrentMc = true;
                                gameVersions.add(gv);
                            }
                            if (!matchesCurrentMc) continue;
                            String gameStr = String.join((CharSequence)", ", gameVersions);
                            boolean isBeta = !ChangelogScreen.isNumericVersion(v);
                            entries.add(new VersionEntry(v, gameStr, isBeta, changelog));
                            if (isBeta || latestNumeric != null && ChangelogScreen.compareVersions(v, latestNumeric) <= 0) continue;
                            latestNumeric = v;
                        }
                        entries.sort((a, b) -> {
                            if (a.isBeta != b.isBeta) {
                                return a.isBeta ? 1 : -1;
                            }
                            return ChangelogScreen.compareVersions(b.version, a.version);
                        });
                        VersionEntry current = new VersionEntry(CURRENT_VERSION, "", false, "");
                        if (!entries.contains(current)) {
                            entries.add(0, current);
                        }
                        this.allVersions = entries;
                        this.loadedCount = entries.size();
                        if (latestNumeric != null) {
                            int cmp = ChangelogScreen.compareVersions(latestNumeric, CURRENT_VERSION);
                            if (cmp > 0) {
                                this.versionStatus = "New update available!";
                                this.versionColor = -757066;
                            } else {
                                this.versionStatus = "You're up to date";
                                this.versionColor = -13315175;
                            }
                            break block10;
                        }
                        this.versionStatus = "All good!";
                        this.versionColor = -13315175;
                    }
                    catch (Exception e) {
                        this.versionStatus = "Loaded with errors";
                        this.versionColor = -290244;
                    }
                } else {
                    this.versionStatus = "Connection failed";
                    this.versionColor = -290244;
                }
            }
        })).exceptionally(e -> {
            this.versionStatus = "No internet connection";
            this.versionColor = -290244;
            return null;
        });
    }

    private static boolean isNumericVersion(String v) {
        for (String p : v.split("\\.")) {
            try {
                Integer.parseInt(p);
            }
            catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    static int compareVersions(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; ++i) {
            int numB;
            int numA = i < partsA.length ? ChangelogScreen.parseOr(partsA[i], -1) : 0;
            int n = numB = i < partsB.length ? ChangelogScreen.parseOr(partsB[i], -1) : 0;
            if (numA == numB) continue;
            return Integer.compare(numA, numB);
        }
        return 0;
    }

    private static int parseOr(String s, int def) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private void drawRoundedPane(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        int s = 4;
        g.fill(x + s, y, x + w - s, y + s, c);
        g.fill(x, y + s, x + s, y + h - s, c);
        g.fill(x + w - s, y + s, x + w, y + h - s, c);
        g.fill(x + s, y + h - s, x + w - s, y + h, c);
        g.fill(x + s, y + s, x + w - s, y + h - s, c);
    }

    private void drawRoundedBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int topC, int sideC, int botC) {
        int s = 4;
        g.fill(x + s, y, x + w - s, y + 1, topC);
        g.fill(x, y + s, x + 1, y + h - s, sideC);
        g.fill(x + w - 1, y + s, x + w, y + h - s, sideC);
        g.fill(x + s, y + h - 1, x + w - s, y + h, botC);
        g.fill(x + s, y + 1, x + s + 1, y + s, topC);
        g.fill(x + w - s - 1, y + 1, x + w - s, y + s, topC);
        g.fill(x + s, y + h - s, x + s + 1, y + h - 1, botC);
        g.fill(x + w - s - 1, y + h - s, x + w - s, y + h - 1, botC);
        g.fill(x, y + s, x + 1, y + s + 1, topC);
        g.fill(x + w - 1, y + s, x + w, y + s + 1, topC);
        g.fill(x, y + h - s - 1, x + 1, y + h - s, botC);
        g.fill(x + w - 1, y + h - s - 1, x + w, y + h - s, botC);
    }

    private int darken(int c) {
        int r = (c >> 16 & 0xFF) * 2 / 3;
        int g2 = (c >> 8 & 0xFF) * 2 / 3;
        int b2 = (c & 0xFF) * 2 / 3;
        return 0xFF000000 | r << 16 | g2 << 8 | b2;
    }

    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.fill(0, 0, this.width, this.height, -401797868);
        int bg = -15920873;
        int headerBg = -15328478;
        int row1 = -15920873;
        int row2 = -15328478;
        int border = -13617603;
        int accent = -8635667;
        int accentBright = -5796870;
        int greenBright = -13315175;
        int pinkBright = -757066;
        int orangeBright = -290244;
        int textMuted = -7629666;
        int textNormal = -3550759;
        int textBright = -985348;
        this.drawRoundedPane(g, this.panelX, this.panelY, this.panelW, this.panelH, bg);
        this.drawRoundedBorder(g, this.panelX, this.panelY, this.panelW, this.panelH, accent, border, border);
        g.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH - 1, border);
        g.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 88, headerBg);
        g.fill(this.panelX, this.panelY + 87, this.panelX + this.panelW, this.panelY + 88, border);
        g.text(this.font, (Component)Component.literal((String)"X toXray"), this.panelX + 24, this.panelY + 16, textBright);
        int statusW = this.font.width(this.versionStatus);
        g.text(this.font, (Component)Component.literal((String)this.versionStatus), this.panelX + 24, this.panelY + 32, this.versionColor);
        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
        String loaderName = FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getName();
        String filterText = "Showing only versions compatible with your " + loaderName + " and Minecraft " + mcVersion;
        g.text(this.font, (Component)Component.literal((String)filterText), this.panelX + 24, this.panelY + 48, textMuted);
        String versionLabel = "v" + CURRENT_VERSION;
        g.text(this.font, (Component)Component.literal((String)versionLabel), this.panelX + this.panelW - 24 - this.font.width(versionLabel), this.panelY + 16, textMuted);
        String cntText = this.loadedCount > 0 ? this.loadedCount + " releases" : "";
        g.text(this.font, (Component)Component.literal((String)cntText), this.panelX + this.panelW - 24 - this.font.width(cntText), this.panelY + 32, textMuted);
        int checkX = this.panelX + 24;
        int checkY = this.panelY + 64;
        g.fill(checkX, checkY, checkX + 12, checkY + 12, this.showBeta ? accent : -14604755);
        if (this.showBeta) {
            g.fill(checkX + 1, checkY + 1, checkX + 11, checkY + 11, -15920873);
            g.text(this.font, (Component)Component.literal((String)"\u2713"), checkX + 3, checkY + 2, accentBright);
        }
        g.text(this.font, (Component)Component.literal((String)"Show beta"), checkX + 16, checkY + 2, this.showBeta ? textBright : textMuted);
        int tableRight = this.tableX + this.colWVersion + 8 + this.colWGames + 8 + this.colWChangelog;
        g.enableScissor(this.tableX, this.tableAreaTop, tableRight, this.tableAreaBottom);
        this.clickTargets.clear();
        List<VersionEntry> versions = this.allVersions;
        int totalRows = 0;
        if (versions != null) {
            for (VersionEntry e : versions) {
                if (e.isBeta && !this.showBeta) continue;
                ++totalRows;
            }
        }
        int contentH = totalRows * 28;
        this.maxScroll = Math.max(0, contentH - (this.tableAreaBottom - this.tableAreaTop));
        this.scrollOffset = Math.max(0.0f, Math.min(this.scrollOffset, this.maxScroll));
        int rowY = this.tableAreaTop - (int)this.scrollOffset;
        this.hoveredRow = -1;
        if (versions != null) {
            int visibleIdx = 0;
            for (int i = 0; i < versions.size(); ++i) {
                boolean isHovered;
                VersionEntry entry = versions.get(i);
                if (entry.isBeta && !this.showBeta) continue;
                if (rowY + 28 < this.tableAreaTop) {
                    rowY += 28;
                    ++visibleIdx;
                    continue;
                }
                if (rowY > this.tableAreaBottom) break;
                boolean bl = isHovered = mouseX >= this.tableX && mouseX <= tableRight && mouseY >= rowY && mouseY < rowY + 28;
                if (isHovered) {
                    this.hoveredRow = visibleIdx;
                }
                boolean isCurrent = entry.version.equals(CURRENT_VERSION);
                int rowBg = isHovered ? -14735049 : ((visibleIdx & 1) == 0 ? row1 : row2);
                g.fill(this.tableX, rowY, tableRight, rowY + 28, rowBg);
                if (isCurrent) {
                    g.fill(this.tableX, rowY, this.tableX + 3, rowY + 28, greenBright);
                } else if (entry.isBeta) {
                    g.fill(this.tableX, rowY, this.tableX + 3, rowY + 28, pinkBright);
                }
                int vColor = isCurrent ? greenBright : (entry.isBeta ? pinkBright : textNormal);
                Object vText = isCurrent ? entry.version + "  current" : entry.version;
                g.text(this.font, (Component)Component.literal((String)vText), this.tableX + 8, rowY + 9, vColor);
                String gameText = entry.gameVersions;
                int gw = this.font.width(gameText);
                g.text(this.font, (Component)Component.literal((String)gameText), this.tableX + this.colWVersion + 8 + (this.colWGames - gw) / 2, rowY + 9, textMuted);
                String preview = entry.changelog;
                int col3X = this.tableX + this.colWVersion + 8 + this.colWGames + 8;
                int previewW = this.colWChangelog - 8;
                if (preview == null || preview.isEmpty()) {
                    g.text(this.font, (Component)Component.literal((String)"-"), col3X + 4, rowY + 9, -11840157);
                } else if (this.font.width(preview = preview.replace('\n', ' ').replace('\r', ' ').trim()) > previewW - this.font.width("Read more") - 4) {
                    String test;
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < preview.length() && this.font.width(test = sb.toString() + preview.charAt(k) + "...") <= previewW - this.font.width("Read more") - 4; ++k) {
                        sb.append(preview.charAt(k));
                    }
                    String truncated = sb.toString().trim();
                    g.text(this.font, (Component)Component.literal((String)(truncated + "... ")), col3X + 4, rowY + 9, textMuted);
                    int rmX = col3X + 4 + this.font.width(truncated + "... ");
                    this.clickTargets.add(new ClickTarget(rmX, rowY + 1, this.font.width("Read more"), 26, entry.version, entry.changelog));
                    g.text(this.font, (Component)Component.literal((String)"Read more"), rmX, rowY + 9, isHovered ? accentBright : -12877066);
                } else {
                    g.text(this.font, (Component)Component.literal((String)preview), col3X + 4, rowY + 9, textMuted);
                }
                g.fill(this.tableX, rowY + 28, tableRight, rowY + 28 + 1, -14604755);
                rowY += 28;
                ++visibleIdx;
            }
        }
        g.disableScissor();
        if (this.maxScroll > 2.0f) {
            float thumbH = Math.max(24.0f, (float)(this.tableAreaBottom - this.tableAreaTop) / (float)(contentH + (this.tableAreaBottom - this.tableAreaTop)) * (float)this.scrollbarH);
            float thumbY = (float)this.scrollbarY + this.scrollOffset / this.maxScroll * ((float)this.scrollbarH - thumbH);
            int sblX = this.scrollbarX;
            int sblW = 12;
            g.fill(sblX, this.scrollbarY, sblX + sblW, this.scrollbarY + this.scrollbarH, -14604755);
            g.fill(sblX, (int)thumbY, sblX + sblW, (int)(thumbY + thumbH), -12038312);
            g.fill(sblX, (int)thumbY, sblX + sblW, (int)thumbY + 2, -9735552);
            g.fill(sblX, (int)(thumbY + thumbH - 2.0f), sblX + sblW, (int)(thumbY + thumbH), -13617603);
        }
        g.fill(this.panelX, this.tableAreaBottom + 1, this.panelX + this.panelW, this.tableAreaBottom + 2, -14604755);
        g.fill(this.btnContinueX, this.btnContinueY, this.btnContinueX + this.btnContinueW, this.btnContinueY + this.btnContinueH, -8635667);
        g.fill(this.btnContinueX, this.btnContinueY, this.btnContinueX + this.btnContinueW, this.btnContinueY + 1, -5796870);
        g.fill(this.btnContinueX, this.btnContinueY + this.btnContinueH - 1, this.btnContinueX + this.btnContinueW, this.btnContinueY + this.btnContinueH, -10804810);
        g.fill(this.btnContinueX, this.btnContinueY, this.btnContinueX + 1, this.btnContinueY + this.btnContinueH, -6330369);
        g.fill(this.btnContinueX + this.btnContinueW - 1, this.btnContinueY, this.btnContinueX + this.btnContinueW, this.btnContinueY + this.btnContinueH, -10804810);
        g.centeredText(this.font, (Component)Component.literal((String)"Continue to Game"), this.btnContinueX + this.btnContinueW / 2, this.btnContinueY + 9, -1);
        g.fill(this.btnDiscordX, this.btnDiscordY, this.btnDiscordX + this.btnDiscordW, this.btnDiscordY + this.btnDiscordH, -14604755);
        g.fill(this.btnDiscordX, this.btnDiscordY, this.btnDiscordX + this.btnDiscordW, this.btnDiscordY + 1, -12038312);
        g.fill(this.btnDiscordX, this.btnDiscordY + this.btnDiscordH - 1, this.btnDiscordX + this.btnDiscordW, this.btnDiscordY + this.btnDiscordH, -15328478);
        g.fill(this.btnDiscordX, this.btnDiscordY, this.btnDiscordX + 1, this.btnDiscordY + this.btnDiscordH, -12038312);
        g.fill(this.btnDiscordX + this.btnDiscordW - 1, this.btnDiscordY, this.btnDiscordX + this.btnDiscordW, this.btnDiscordY + this.btnDiscordH, -15328478);
        g.centeredText(this.font, (Component)Component.literal((String)"Join Discord"), this.btnDiscordX + this.btnDiscordW / 2, this.btnDiscordY + 5, -3550759);
        g.fill(this.btnModrinthX, this.btnModrinthY, this.btnModrinthX + this.btnModrinthW, this.btnModrinthY + this.btnModrinthH, -14604755);
        g.fill(this.btnModrinthX, this.btnModrinthY, this.btnModrinthX + this.btnModrinthW, this.btnModrinthY + 1, -12038312);
        g.fill(this.btnModrinthX, this.btnModrinthY + this.btnModrinthH - 1, this.btnModrinthX + this.btnModrinthW, this.btnModrinthY + this.btnModrinthH, -15328478);
        g.fill(this.btnModrinthX, this.btnModrinthY, this.btnModrinthX + 1, this.btnModrinthY + this.btnModrinthH, -12038312);
        g.fill(this.btnModrinthX + this.btnModrinthW - 1, this.btnModrinthY, this.btnModrinthX + this.btnModrinthW, this.btnModrinthY + this.btnModrinthH, -15328478);
        g.centeredText(this.font, (Component)Component.literal((String)"Modrinth"), this.btnModrinthX + this.btnModrinthW / 2, this.btnModrinthY + 5, -3550759);
        g.fill(this.btnNeverX, this.btnNeverY, this.btnNeverX + this.btnNeverW, this.btnNeverY + this.btnNeverH, -14604755);
        g.fill(this.btnNeverX, this.btnNeverY, this.btnNeverX + this.btnNeverW, this.btnNeverY + 1, -12038312);
        g.fill(this.btnNeverX, this.btnNeverY + this.btnNeverH - 1, this.btnNeverX + this.btnNeverW, this.btnNeverY + this.btnNeverH, -15328478);
        g.fill(this.btnNeverX, this.btnNeverY, this.btnNeverX + 1, this.btnNeverY + this.btnNeverH, -12038312);
        g.fill(this.btnNeverX + this.btnNeverW - 1, this.btnNeverY, this.btnNeverX + this.btnNeverW, this.btnNeverY + this.btnNeverH, -15328478);
        g.centeredText(this.font, (Component)Component.literal((String)"Never show again"), this.btnNeverX + this.btnNeverW / 2, this.btnNeverY + 5, -3550759);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int)event.x();
            int my = (int)event.y();
            int checkX = this.panelX + 24;
            int checkY = this.panelY + 64;
            if (mx >= checkX && mx <= checkX + 12 && my >= checkY && my <= checkY + 12) {
                this.showBeta = !this.showBeta;
                return true;
            }
            if (mx >= this.btnContinueX && mx <= this.btnContinueX + this.btnContinueW && my >= this.btnContinueY && my <= this.btnContinueY + this.btnContinueH) {
                this.onClose();
                return true;
            }
            if (mx >= this.btnDiscordX && mx <= this.btnDiscordX + this.btnDiscordW && my >= this.btnDiscordY && my <= this.btnDiscordY + this.btnDiscordH) {
                Util.getPlatform().openUri(DISCORD_URI);
                return true;
            }
            if (mx >= this.btnModrinthX && mx <= this.btnModrinthX + this.btnModrinthW && my >= this.btnModrinthY && my <= this.btnModrinthY + this.btnModrinthH) {
                Util.getPlatform().openUri(MODRINTH_URI);
                return true;
            }
            if (mx >= this.btnNeverX && mx <= this.btnNeverX + this.btnNeverW && my >= this.btnNeverY && my <= this.btnNeverY + this.btnNeverH) {
                XrayState.getInstance().setNeverShowChangelog(true);
                this.onClose();
                return true;
            }
            for (ClickTarget t : this.clickTargets) {
                if (mx < t.x || mx > t.x + t.w || my < t.y || my > t.y + t.h) continue;
                Minecraft.getInstance().setScreen(new ChangelogDetailScreen(this, t.version, t.changelog));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll > 0.0f) {
            this.scrollOffset = Math.max(0.0f, Math.min(this.scrollOffset - (float)verticalAmount * 28.0f * 0.5f, this.maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    static class VersionEntry {
        final String version;
        final String gameVersions;
        final boolean isBeta;
        final String changelog;

        VersionEntry(String version, String gameVersions, boolean isBeta, String changelog) {
            this.version = version;
            this.gameVersions = gameVersions;
            this.isBeta = isBeta;
            this.changelog = changelog;
        }

        public boolean equals(Object o) {
            return o instanceof VersionEntry && this.version.equals(((VersionEntry)o).version);
        }

        public int hashCode() {
            return this.version.hashCode();
        }
    }

    private record ClickTarget(int x, int y, int w, int h, String version, String changelog) {
    }
}



