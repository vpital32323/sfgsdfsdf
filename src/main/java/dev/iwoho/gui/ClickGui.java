package dev.iwoho.gui;

import dev.iwoho.IwohoClient;
import dev.iwoho.Module;
import dev.iwoho.Module.Category;
import dev.iwoho.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Screen {
    private static final int COL_W = 130, GAP = 8, HEAD = 24, ROW = 20, SROW = 18;
    private static final float[] PRESETS = {0, 30, 120, 190, 265, 320};

    private record Hit(int x1, int y1, int x2, int y2, Module m, Module.Setting s, int kind, float v) {
        boolean in(double x, double y) { return x >= x1 && x < x2 && y >= y1 && y < y2; }
    }

    private final long openedAt = Util.getMeasuringTimeMs();
    private long lastFrame = openedAt;
    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    private String search = "";

    public ClickGui() { super(Text.literal("iwoho client")); }

    @Override public boolean shouldPause() { return false; }

    // ───────── helpers ─────────
    private static float ease(float t) { float f = 1 - t; return 1 - f * f * f; }
    private static float lerp(float a, float b, float t) { return a + (b - a) * Math.min(1f, t); }
    private static int argb(int a, int rgb) { return (MathHelper.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF); }
    private static int mix(int c1, int c2, float t) {
        int r = (int) lerp((c1 >> 16) & 255, (c2 >> 16) & 255, t);
        int g = (int) lerp((c1 >> 8) & 255, (c2 >> 8) & 255, t);
        int b = (int) lerp(c1 & 255, c2 & 255, t);
        return (r << 16) | (g << 8) | b;
    }
    private void text(DrawContext c, String s, int x, int y, int rgb, int a) {
        if (a < 8) return;
        c.drawTextWithShadow(textRenderer, s, x, y, argb(a, rgb));
    }

    // ───────── render ─────────
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long now = Util.getMeasuringTimeMs();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float open = ease(Math.min(1f, (now - openedAt) / 280f));
        int a = (int) (open * 255);
        hits.clear();

        c.fill(0, 0, width, height, argb((int) (open * 110), 0x000000));

        var ms = c.getMatrices();
        ms.push();
        float sc = 0.92f + 0.08f * open;
        ms.translate(width / 2f, height / 2f, 0);
        ms.scale(sc, sc, 1);
        ms.translate(-width / 2f, -height / 2f, 0);

        int accent = IwohoClient.accent();
        Category[] cats = Category.values();
        int total = cats.length * COL_W + (cats.length - 1) * GAP;
        int x = (width - total) / 2;
        int top = Math.max(16, height / 2 - 130);
        for (Category cat : cats) {
            drawColumn(c, cat, x, top, mx, my, dt, a, accent);
            x += COL_W + GAP;
        }
        drawSearch(c, a, accent, now);
        drawTheme(c, a, accent);
        ms.pop();
    }

    private void drawColumn(DrawContext c, Category cat, int x, int y, int mx, int my, float dt, int a, int accent) {
        String q = search.toLowerCase();
        List<Module> mods = ModuleManager.ALL.stream()
                .filter(m -> m.category == cat && (q.isEmpty() || m.name.toLowerCase().contains(q))).toList();

        float body = 0;
        for (Module m : mods) body += ROW + m.expandAnim * m.settings.size() * SROW;
        int h = (int) (HEAD + body + 6);

        c.fill(x, y, x + COL_W, y + h, argb((int) (a * 0.93f), 0x101216));
        c.fill(x, y, x + COL_W, y + HEAD, argb(a, 0x171a21));
        c.fillGradient(x, y + HEAD - 2, x + COL_W, y + HEAD, argb(a, accent), argb(a / 4, accent));
        text(c, cat.label, x + 8, y + 8, 0xFFFFFF, a);

        int ry = y + HEAD + 3;
        for (Module m : mods) {
            boolean hov = mx >= x + 4 && mx < x + COL_W - 4 && my >= ry && my < ry + ROW - 2;
            m.hoverAnim = lerp(m.hoverAnim, hov ? 1 : 0, dt * 14);
            m.toggleAnim = lerp(m.toggleAnim, m.enabled ? 1 : 0, dt * 12);
            m.expandAnim = lerp(m.expandAnim, m.expanded ? 1 : 0, dt * 12);

            c.fill(x + 4, ry, x + COL_W - 4, ry + ROW - 2, argb(a, mix(0x1b1e25, 0x272b35, m.hoverAnim)));
            int ea = (int) (a * m.toggleAnim * 0.55f);
            c.fillGradient(x + 4, ry, x + COL_W - 4, ry + ROW - 2, argb(ea, accent), argb(ea / 3, accent));
            c.fill(x + 4, ry, x + 6, ry + ROW - 2, argb((int) (a * m.toggleAnim), accent));
            text(c, m.name, x + 11 + (int) (m.hoverAnim * 2), ry + 6, mix(0x9ea4ae, 0xFFFFFF, m.toggleAnim), a);
            if (!m.settings.isEmpty()) text(c, m.expanded ? "-" : "+", x + COL_W - 14, ry + 6, 0x808590, a);
            hits.add(new Hit(x + 4, ry, x + COL_W - 4, ry + ROW - 2, m, null, 0, 0));

            int sy = ry + ROW;
            int vis = (int) (m.expandAnim * m.settings.size() * SROW);
            if (vis > 1) {
                c.enableScissor(x + 4, sy - 2, x + COL_W - 4, sy + vis - 2);
                for (Module.Setting s : m.settings) {
                    c.fill(x + 4, sy - 2, x + COL_W - 4, sy + SROW - 2, argb(a, 0x15181e));
                    text(c, s.name, x + 10, sy + 1, 0xb8bcc6, a);
                    String val = String.format("%.1f", s.value);
                    text(c, val, x + COL_W - 10 - textRenderer.getWidth(val), sy + 1, 0x8a909b, a);
                    int tx1 = x + 10, tx2 = x + COL_W - 10, ty = sy + 12;
                    float r = (float) ((s.value - s.min) / (s.max - s.min));
                    c.fill(tx1, ty, tx2, ty + 2, argb(a, 0x2a2e38));
                    c.fillGradient(tx1, ty, tx1 + (int) ((tx2 - tx1) * r), ty + 2, argb(a, accent), argb(a, mix(accent, 0xFFFFFF, 0.4f)));
                    c.fill(tx1 + (int) ((tx2 - tx1) * r) - 1, ty - 1, tx1 + (int) ((tx2 - tx1) * r) + 2, ty + 3, argb(a, 0xFFFFFF));
                    if (m.expandAnim > 0.9f)
                        hits.add(new Hit(tx1, sy, tx2, sy + SROW - 2, m, s, 1, 0));
                    sy += SROW;
                }
                c.disableScissor();
            }
            ry += ROW + vis;
        }
    }

    private void drawSearch(DrawContext c, int a, int accent, long now) {
        int w = 180, x = (width - w) / 2, y = height - 46;
        c.fill(x, y, x + w, y + 20, argb((int) (a * 0.93f), 0x101216));
        c.fill(x, y + 19, x + w, y + 20, argb(a, accent));
        boolean caret = (now / 500) % 2 == 0;
        if (search.isEmpty()) text(c, "Search...", x + 8, y + 6, 0x6c727d, a);
        else text(c, search + (caret ? "_" : ""), x + 8, y + 6, 0xFFFFFF, a);
    }

    private void drawTheme(DrawContext c, int a, int accent) {
        int w = 160, hgt = 74, x = width - w - 10, y = height - hgt - 10;
        c.fill(x, y, x + w, y + hgt, argb((int) (a * 0.93f), 0x101216));
        c.fill(x, y, x + w, y + 20, argb(a, 0x171a21));
        c.fillGradient(x, y + 18, x + w, y + 20, argb(a, accent), argb(a / 4, accent));
        text(c, "Theme color", x + 8, y + 6, 0xFFFFFF, a);

        int bx = x + 10, bw = w - 20, by = y + 30;
        for (int i = 0; i < bw; i++)
            c.fill(bx + i, by, bx + i + 1, by + 8, argb(a, MathHelper.hsvToRgb(i / (float) bw, 0.62f, 1f)));
        int mxp = bx + (int) (IwohoClient.THEME_HUE.value / 360.0 * bw);
        c.fill(mxp - 1, by - 3, mxp + 2, by + 11, argb(a, 0xFFFFFF));
        hits.add(new Hit(bx, by - 4, bx + bw, by + 12, null, IwohoClient.THEME_HUE, 2, 0));

        int px = bx;
        for (float hue : PRESETS) {
            int col = MathHelper.hsvToRgb(hue / 360f, 0.62f, 1f);
            c.fill(px, y + 50, px + 18, y + 66, argb(a, col));
            hits.add(new Hit(px, y + 50, px + 18, y + 66, null, null, 3, hue));
            px += 24;
        }
    }

    // ───────── input ─────────
    private void slide(Hit h, double mx) {
        double r = MathHelper.clamp((mx - h.x1) / (double) (h.x2 - h.x1), 0, 1);
        double v = h.s.min + (h.s.max - h.s.min) * r;
        h.s.value = Math.round(v * 10) / 10.0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(mx, my)) continue;
            switch (h.kind) {
                case 0 -> {
                    if (btn == 0) h.m.toggle();
                    else if (btn == 1 && !h.m.settings.isEmpty()) h.m.expanded = !h.m.expanded;
                }
                case 1, 2 -> { if (btn == 0) { dragging = h; slide(h, mx); } }
                case 3 -> IwohoClient.THEME_HUE.value = h.v;
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging != null) { slide(dragging, mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (dragging != null) {
            if (dragging.m != null) dragging.m.onSettingChanged();
            dragging = null;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (chr >= 32 && search.length() < 20) { search += chr; return true; }
        return super.charTyped(chr, mods);
    }
}
