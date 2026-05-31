package com.xtoxray.client.gui;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class ChangelogDetailScreen extends Screen {
    private static final int MARGIN = 20;
    private static final int TEXT_COLOR = -3355444;
    private static final int TITLE_COLOR = -11751600;
    private static final int LINK_COLOR = -12409355;
    private static final int CODE_COLOR = -1683200;
    private static final int CODE_BG = -14671840;
    private static final int TABLE_BORDER = -11184811;
    private static final int DETAILS_SUMMARY_COLOR = -8892;
    private final Screen parent;
    private final String versionNumber;
    private final String changelog;
    private float scrollOffset;
    private float maxScroll;
    private int contentHeight;
    private List<Chunk> chunks;
    private final List<LinkRect> linkRects = new ArrayList<>();
    private final List<DetailsRect> detailsRects = new ArrayList<>();
    private final Set<Integer> expandedDetails = new HashSet<>();
    private final Map<String, ImageEntry> imageCache = new HashMap<>();
    private boolean imagesLoaded;

    public ChangelogDetailScreen(Screen parent, String versionNumber, String changelog) {
        super(Component.literal("Changelog - " + versionNumber));
        this.parent = parent;
        this.versionNumber = versionNumber;
        this.changelog = changelog != null && !changelog.isEmpty() ? changelog : "No changelog available.";
    }

    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.onClose()).bounds(cx - 50, this.height - 28, 100, 20).build());
        int wrapWidth = this.width - 40;
        this.chunks = parseChunks(this.changelog, wrapWidth, new int[]{0});
        this.startImageDownloads();
        this.contentHeight = computeContentHeight();
        int textAreaHeight = this.height - 62;
        this.maxScroll = Math.max(0, this.contentHeight - textAreaHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
    }

    private List<Chunk> parseChunks(String text, int wrapWidth, int[] idCounter) {
        List<Chunk> result = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) {
                i++;
                continue;
            }
            if (trimmed.matches("^[-*_]{3,}$")) {
                result.add(new RuleChunk());
                i++;
                continue;
            }
            if (trimmed.startsWith("```")) {
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    if (code.length() > 0) code.append("\n");
                    code.append(lines[i]);
                    i++;
                }
                if (i < lines.length) i++;
                result.add(new CodeChunk(code.toString()));
                continue;
            }
            if (trimmed.equals("<details>")) {
                i++;
                String summary = "";
                if (i < lines.length) {
                    String sl = lines[i].trim();
                    if (sl.startsWith("<summary>")) {
                        int close = sl.lastIndexOf("</summary>");
                        summary = close > 0 ? sl.substring(9, close) : sl.substring(9);
                        i++;
                    }
                }
                List<String> contentLines = new ArrayList<>();
                while (i < lines.length && !lines[i].trim().equals("</details>")) {
                    contentLines.add(lines[i]);
                    i++;
                }
                if (i < lines.length) i++;
                int id = idCounter[0]++;
                result.add(new DetailsChunk(summary, parseChunks(String.join("\n", contentLines), wrapWidth, idCounter), id));
                continue;
            }
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                List<String> tableLines = new ArrayList<>();
                while (i < lines.length && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim());
                    i++;
                }
                result.add(parseTableBlock(tableLines));
                continue;
            }
            if (trimmed.matches("^!\\[.*\\]\\(.*\\)$")) {
                int parenStart = trimmed.lastIndexOf("](");
                int urlEnd = trimmed.lastIndexOf(")");
                if (parenStart != -1 && urlEnd > parenStart) {
                    String fullUrl = trimmed.substring(parenStart + 2, urlEnd);
                    int spaceIdx = fullUrl.indexOf(' ');
                    String url = spaceIdx > 0 ? fullUrl.substring(0, spaceIdx) : fullUrl;
                    result.add(new ImageChunk(url));
                }
                i++;
                continue;
            }
            List<String> paraLines = new ArrayList<>();
            while (i < lines.length) {
                String t = lines[i].trim();
                if (t.isEmpty()) break;
                if (t.startsWith("```") || t.equals("<details>") || t.equals("</details>") ||
                    (t.startsWith("|") && t.endsWith("|")) || t.matches("^[-*_]{3,}$")) break;
                paraLines.add(lines[i]);
                i++;
            }
            if (!paraLines.isEmpty()) {
                result.add(parseTextBlock(paraLines, wrapWidth));
            }
        }
        return result;
    }

    private TextChunk parseTextBlock(List<String> lines, int wrapWidth) {
        List<StyledLine> styledLines = new ArrayList<>();
        for (String raw : lines) {
            String trimmed = raw.trim();
            int indent = 0;
            for (char c : raw.toCharArray()) {
                if (c == ' ') indent++;
                else break;
            }
            if (trimmed.startsWith("### ")) {
                styledLines.add(new StyledLine(trimmed.substring(4), indent, -10044566, false, false, 9));
                continue;
            }
            if (trimmed.startsWith("## ")) {
                styledLines.add(new StyledLine(trimmed.substring(3), indent, -8892, true, false, 9));
                continue;
            }
            if (trimmed.startsWith("# ")) {
                styledLines.add(new StyledLine(trimmed.substring(2), indent, -22016, true, false, 13));
                continue;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                styledLines.add(new StyledLine("\u2022 " + trimmed.substring(2), indent + 10, 0, false, false, 9));
                continue;
            }
            if (trimmed.matches("^\\d+[.)]\\s.*")) {
                styledLines.add(new StyledLine(trimmed, indent + 10, 0, false, false, 9));
                continue;
            }
            if (trimmed.startsWith("> ")) {
                styledLines.add(new StyledLine(trimmed.substring(2), indent + 10, -5592406, false, true, 9));
                continue;
            }
            if (trimmed.startsWith(">> ")) {
                styledLines.add(new StyledLine(trimmed.substring(3), indent + 20, -5592406, false, true, 9));
                continue;
            }
            if (trimmed.equals(">")) {
                styledLines.add(new StyledLine("", indent, 0, false, false, 5));
                continue;
            }
            String[] words = trimmed.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) continue;
                String test = line.isEmpty() ? word : line + " " + word;
                if (this.font.width(test) > wrapWidth && !line.isEmpty()) {
                    styledLines.add(new StyledLine(line.toString(), indent, 0, false, false, 9));
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) {
                styledLines.add(new StyledLine(line.toString(), indent, 0, false, false, 9));
            }
        }
        return new TextChunk(styledLines);
    }

    private TableChunk parseTableBlock(List<String> lines) {
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.matches("^[|\\s:-]+$")) continue;
            String content = line.substring(1, line.length() - 1);
            String[] cells = content.split("\\|", -1);
            for (int j = 0; j < cells.length; j++) {
                cells[j] = cells[j].trim();
            }
            rows.add(cells);
        }
        int maxCols = 0;
        for (String[] row : rows) maxCols = Math.max(maxCols, row.length);
        int[] colWidths = new int[maxCols];
        for (String[] row : rows) {
            for (int j = 0; j < row.length; j++) {
                colWidths[j] = Math.max(colWidths[j], this.font.width(row[j]));
            }
        }
        for (int j = 0; j < maxCols; j++) colWidths[j] += 8;
        int height = rows.size() * 12 + 4;
        return new TableChunk(rows, colWidths, height);
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        graphics.centeredText(this.font, Component.literal("Changelog - " + this.versionNumber), cx, 16, TITLE_COLOR);
        int textAreaTop = 30;
        int textAreaBottom = this.height - 32;
        if (textAreaBottom <= textAreaTop) return;
        graphics.enableScissor(MARGIN, textAreaTop, this.width - MARGIN * 2, textAreaBottom);
        this.linkRects.clear();
        this.detailsRects.clear();
        int y = textAreaTop + 4 - (int) this.scrollOffset;
        for (Chunk chunk : this.chunks) {
            if (y > textAreaBottom) break;
            int used = renderChunk(graphics, chunk, y, mouseX, mouseY, textAreaTop, textAreaBottom);
            if (used > 0) y += used;
        }
        graphics.disableScissor();
    }

    private int renderChunk(GuiGraphicsExtractor graphics, Chunk chunk, int y, int mouseX, int mouseY, int clipTop, int clipBottom) {
        return switch (chunk) {
            case TextChunk tc -> renderTextChunk(graphics, tc, y, mouseX, mouseY, clipTop, clipBottom);
            case CodeChunk cc -> renderCodeChunk(graphics, cc, y, clipTop, clipBottom);
            case RuleChunk rc -> renderRule(graphics, y, clipTop, clipBottom);
            case TableChunk tc -> renderTableChunk(graphics, tc, y, mouseX, mouseY, clipTop, clipBottom);
            case DetailsChunk dc -> renderDetailsChunk(graphics, dc, y, mouseX, mouseY, clipTop, clipBottom);
            case ImageChunk ic -> renderImageChunk(graphics, ic, y, clipTop, clipBottom);
        };
    }

    private int renderTextChunk(GuiGraphicsExtractor graphics, TextChunk tc, int y, int mouseX, int mouseY, int clipTop, int clipBottom) {
        if (y + getTextBlockHeight(tc) < clipTop) return getTextBlockHeight(tc);
        int startY = y;
        for (StyledLine sl : tc.lines) {
            if (y + sl.height >= clipTop && y <= clipBottom) {
                int segX = MARGIN + sl.indent;
                List<Segment> segs = parseInline(sl.text, sl.color, sl.bold, sl.italic);
                for (Segment seg : segs) {
                    int segW = this.font.width(seg.text);
                    int color = seg.color != 0 ? seg.color : TEXT_COLOR;
                    Component comp = buildComponent(seg);
                    if (seg.url != null) {
                        this.linkRects.add(new LinkRect(segX, y, segW, 9, seg.url));
                    }
                    graphics.text(this.font, comp, segX, y, color);
                    segX += segW;
                }
            }
            y += sl.height;
        }
        return y - startY;
    }

    private int renderCodeChunk(GuiGraphicsExtractor graphics, CodeChunk cc, int y, int clipTop, int clipBottom) {
        int lineH = 9;
        String[] codeLines = cc.code.split("\n");
        int totalH = codeLines.length * lineH + 8;
        if (y + totalH < clipTop || y > clipBottom) return totalH;
        int bx = MARGIN + 2;
        int bw = this.width - MARGIN * 2 - 4;
        int bh = totalH;
        graphics.fill(bx, y, bx + bw, y + bh, CODE_BG);
        int ty = y + 4;
        for (String cl : codeLines) {
            if (ty > clipBottom) break;
            if (ty + lineH >= clipTop) {
                graphics.text(this.font, Component.literal(cl), bx + 4, ty, CODE_COLOR);
            }
            ty += lineH;
        }
        return totalH;
    }

    private int renderRule(GuiGraphicsExtractor graphics, int y, int clipTop, int clipBottom) {
        if (y + 4 < clipTop || y > clipBottom) return 4;
        graphics.fill(MARGIN + 2, y + 1, this.width - MARGIN - 2, y + 2, TABLE_BORDER);
        return 4;
    }

    private int renderTableChunk(GuiGraphicsExtractor graphics, TableChunk tc, int y, int mouseX, int mouseY, int clipTop, int clipBottom) {
        int totalH = tc.height;
        if (y + totalH < clipTop || y > clipBottom) return totalH;
        int rowH = 12;
        int startX = MARGIN + 2;
        int tableX = startX;
        for (int ri = 0; ri < tc.rows.size(); ri++) {
            int ry = y + ri * rowH;
            if (ry + rowH < clipTop || ry > clipBottom) continue;
            String[] row = tc.rows.get(ri);
            int cellX = startX;
            for (int j = 0; j < tc.colWidths.length; j++) {
                int cw = tc.colWidths[j];
                if (ri == 0) {
                    graphics.fill(cellX - 1, ry, cellX + cw, ry + 1, TABLE_BORDER);
                    if (j == tc.colWidths.length - 1) graphics.fill(cellX + cw, ry, cellX + cw + 1, ry + rowH, TABLE_BORDER);
                }
                graphics.fill(cellX - 1, ry, cellX, ry + rowH, TABLE_BORDER);
                graphics.fill(cellX - 1, ry + rowH - 1, cellX + cw, ry + rowH, TABLE_BORDER);
                if (j < row.length && !row[j].isEmpty()) {
                    graphics.text(this.font, Component.literal(row[j]), cellX + 2, ry + 2, TEXT_COLOR);
                }
                cellX += cw;
            }
        }
        return totalH;
    }

    private int renderDetailsChunk(GuiGraphicsExtractor graphics, DetailsChunk dc, int y, int mouseX, int mouseY, int clipTop, int clipBottom) {
        boolean expanded = this.expandedDetails.contains(dc.id);
        int summaryTextHeight = 9;
        int summaryH = summaryTextHeight + 6;
        String prefix = expanded ? "[-] " : "[+] ";
        String summaryLine = prefix + (dc.summary.isEmpty() ? "Details" : dc.summary);
        if (y + summaryH >= clipTop && y <= clipBottom) {
            List<Segment> segs = parseInline(summaryLine, DETAILS_SUMMARY_COLOR, true, false);
            int segX = MARGIN + 2;
            for (Segment seg : segs) {
                int segW = this.font.width(seg.text);
                graphics.text(this.font, buildComponent(seg), segX, y + 3, DETAILS_SUMMARY_COLOR);
                segX += segW;
            }
            this.detailsRects.add(new DetailsRect(MARGIN + 2, y, this.width - MARGIN * 2 - 4, summaryH, dc.id));
        }
        y += summaryH;
        if (!expanded) return summaryH;
        int contentH = 0;
        for (Chunk sub : dc.content) {
            if (y > clipBottom) break;
            int used = renderChunk(graphics, sub, y, mouseX, mouseY, clipTop, clipBottom);
            if (used > 0) {
                y += used;
                contentH += used;
            }
        }
        return summaryH + contentH;
    }

    private int computeContentHeight() {
        int h = 0;
        for (Chunk chunk : this.chunks) h += getChunkHeight(chunk);
        return h + 8;
    }

    private int getChunkHeight(Chunk chunk) {
        return switch (chunk) {
            case TextChunk tc -> getTextBlockHeight(tc);
            case CodeChunk cc -> cc.code.split("\n").length * 9 + 8;
            case RuleChunk rc -> 4;
            case TableChunk tc -> tc.height;
            case ImageChunk ic -> 64;
            case DetailsChunk dc -> {
                int h = 15;
                if (this.expandedDetails.contains(dc.id)) {
                    for (Chunk sub : dc.content) h += getChunkHeight(sub);
                }
                yield h;
            }
        };
    }

    private int getTextBlockHeight(TextChunk tc) {
        int h = 0;
        for (StyledLine sl : tc.lines) h += sl.height;
        return h;
    }

    private void recomputeScroll() {
        this.contentHeight = computeContentHeight();
        int textAreaHeight = this.height - 62;
        this.maxScroll = Math.max(0, this.contentHeight - textAreaHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            for (LinkRect lr : this.linkRects) {
                if (mx >= lr.x && mx <= lr.x + lr.w && my >= lr.y && my <= lr.y + lr.h) {
                    Util.getPlatform().openUri(URI.create(lr.url));
                    return true;
                }
            }
            for (DetailsRect dr : this.detailsRects) {
                if (mx >= dr.x && mx <= dr.x + dr.w && my >= dr.y && my <= dr.y + dr.h) {
                    if (this.expandedDetails.contains(dr.id)) {
                        this.expandedDetails.remove(dr.id);
                    } else {
                        this.expandedDetails.add(dr.id);
                    }
                    this.recomputeScroll();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll > 0) {
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset - (float) verticalAmount * 12, this.maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private Component buildComponent(Segment seg) {
        Style s = Style.EMPTY;
        if (seg.bold) s = s.withBold(true);
        if (seg.italic) s = s.withItalic(true);
        if (seg.color != 0) s = s.withColor(seg.color);
        return Component.literal(seg.text).withStyle(s);
    }

    private List<Segment> parseInline(String text, int baseColor, boolean baseBold, boolean baseItalic) {
        List<Segment> parts = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end, labelEnd, urlEnd;
            if (text.startsWith("![", i) && (labelEnd = text.indexOf("]", i + 2)) != -1 &&
                labelEnd + 1 < text.length() && text.charAt(labelEnd + 1) == '(' &&
                (urlEnd = text.indexOf(")", labelEnd + 2)) != -1) {
                String alt = text.substring(i + 2, labelEnd);
                String fullUrl = text.substring(labelEnd + 2, urlEnd);
                int spaceIdx = fullUrl.indexOf(' ');
                String url = spaceIdx > 0 ? fullUrl.substring(0, spaceIdx) : fullUrl;
                parts.add(new Segment("[img:" + alt + "]", -5592406, false, true, url));
                i = urlEnd + 1;
                continue;
            }
            if (text.startsWith("**", i) && (end = text.indexOf("**", i + 2)) != -1) {
                parts.add(new Segment(text.substring(i + 2, end), baseColor, true, baseItalic, null));
                i = end + 2;
                continue;
            }
            if (text.charAt(i) == '*' && !text.startsWith("**", i) && (end = text.indexOf("*", i + 1)) != -1) {
                parts.add(new Segment(text.substring(i + 1, end), baseColor, baseBold, true, null));
                i = end + 1;
                continue;
            }
            if (text.charAt(i) == '_' && (end = text.indexOf("_", i + 1)) != -1) {
                parts.add(new Segment(text.substring(i + 1, end), baseColor, baseBold, true, null));
                i = end + 1;
                continue;
            }
            if (text.charAt(i) == '`' && (end = text.indexOf("`", i + 1)) != -1) {
                parts.add(new Segment(text.substring(i + 1, end), CODE_COLOR, false, false, null));
                i = end + 1;
                continue;
            }
            if (text.startsWith("[", i) && (labelEnd = text.indexOf("]", i + 1)) != -1 &&
                labelEnd + 1 < text.length() && text.charAt(labelEnd + 1) == '(' &&
                (urlEnd = text.indexOf(")", labelEnd + 2)) != -1) {
                String label = text.substring(i + 1, labelEnd);
                String url = text.substring(labelEnd + 2, urlEnd);
                parts.add(new Segment(label, LINK_COLOR, false, false, url));
                i = urlEnd + 1;
                continue;
            }
            int start = i;
            while (i < text.length() && text.charAt(i) != '*' && text.charAt(i) != '_' && text.charAt(i) != '`' && text.charAt(i) != '[' && text.charAt(i) != '!') {
                i++;
            }
            if (i > start) {
                parts.add(new Segment(text.substring(start, i), baseColor, baseBold, baseItalic, null));
                continue;
            }
            parts.add(new Segment(String.valueOf(text.charAt(i)), baseColor, baseBold, baseItalic, null));
            i++;
        }
        return parts;
    }

    private sealed interface Chunk permits TextChunk, CodeChunk, RuleChunk, TableChunk, DetailsChunk, ImageChunk {}
    private record TextChunk(List<StyledLine> lines) implements Chunk {}
    private record CodeChunk(String code) implements Chunk {}
    private record RuleChunk() implements Chunk {}
    private record TableChunk(List<String[]> rows, int[] colWidths, int height) implements Chunk {}
    private record DetailsChunk(String summary, List<Chunk> content, int id) implements Chunk {}
    private record ImageChunk(String url) implements Chunk {}
    private record StyledLine(String text, int indent, int color, boolean bold, boolean italic, int height) {}
    private record Segment(String text, int color, boolean bold, boolean italic, String url) {}
    private record Line(List<Segment> segments, int height) {}
    private record LinkRect(int x, int y, int w, int h, String url) {}
    private record DetailsRect(int x, int y, int w, int h, int id) {}
    private record ImageEntry(Identifier id, int width, int height, boolean loaded) {}

    private void startImageDownloads() {
        for (Chunk chunk : this.chunks) {
            collectImageUrls(chunk);
        }
        if (!this.imageCache.isEmpty()) {
            this.imagesLoaded = false;
        }
    }

    private void collectImageUrls(Chunk chunk) {
        if (chunk instanceof ImageChunk ic) {
            scheduleDownload(ic.url);
        } else if (chunk instanceof TextChunk tc) {
            for (StyledLine sl : tc.lines) {
                String text = sl.text;
                int idx = 0;
                while ((idx = text.indexOf("![", idx)) != -1) {
                    int close = text.indexOf("](", idx + 2);
                    if (close == -1) break;
                    int urlEnd = text.indexOf(")", close + 2);
                    if (urlEnd == -1) break;
                    String fullUrl = text.substring(close + 2, urlEnd);
                    int space = fullUrl.indexOf(' ');
                    String url = space > 0 ? fullUrl.substring(0, space) : fullUrl;
                    scheduleDownload(url);
                    idx = urlEnd + 1;
                }
            }
        } else if (chunk instanceof DetailsChunk dc) {
            for (Chunk sub : dc.content) collectImageUrls(sub);
        }
    }

    private void scheduleDownload(String url) {
        if (this.imageCache.containsKey(url)) return;
        this.imageCache.put(url, new ImageEntry(null, 0, 0, false));
        Util.ioPool().execute(() -> {
            try {
                InputStream in = new URL(url).openStream();
                com.mojang.blaze3d.platform.NativeImage ni = com.mojang.blaze3d.platform.NativeImage.read(in);
                in.close();
                int w = ni.getWidth();
                int h = ni.getHeight();
                Minecraft.getInstance().execute(() -> {
                    DynamicTexture tex = new DynamicTexture(() -> "changelog_img", ni);
                    int idx = this.imageCache.size();
                    Identifier loc = Identifier.parse("xtoxray:changelog_img_" + idx);
                    this.minecraft.getTextureManager().register(loc, tex);
                    this.imageCache.put(url, new ImageEntry(loc, w, h, true));
                    this.imagesLoaded = true;
                });
            } catch (Exception e) {
                System.err.println("[XtoXray] Failed to load image: " + url);
                e.printStackTrace();
            }
        });
    }

    private int renderImageChunk(GuiGraphicsExtractor graphics, ImageChunk ic, int y, int clipTop, int clipBottom) {
        int imgMaxW = this.width - MARGIN * 2 - 4;
        int imgH = 60;
        if (y + imgH < clipTop || y > clipBottom) return imgH;
        ImageEntry entry = this.imageCache.get(ic.url);
        if (entry != null && entry.loaded()) {
            int iw = entry.width();
            int ih = entry.height();
            float scale = Math.min((float) imgMaxW / iw, 120.0f / ih);
            scale = Math.min(scale, 1.0f);
            int rw = (int) (iw * scale);
            int rh = (int) (ih * scale);
            int rx = MARGIN + 2 + (imgMaxW - rw) / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, entry.id(), rx, y, rw, rh, 0, 0, 1, 1);
        } else {
            graphics.text(this.font, Component.literal("[Loading image...]"), MARGIN + 2, y + imgH / 2 - 4, TEXT_COLOR);
        }
        return imgH;
    }
}
