package dev.iwoho;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), WORLD("World");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public static class Setting {
        public final String name;
        public final double min, max;
        public double value;
        public Setting(String name, double min, double max, double value) {
            this.name = name; this.min = min; this.max = max; this.value = value;
        }
    }

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final Category category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    // анимации GUI
    public float hoverAnim, toggleAnim, expandAnim;
    public boolean expanded;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public Setting add(String n, double min, double max, double v) {
        Setting s = new Setting(n, min, max, v);
        settings.add(s);
        return s;
    }

    public double get(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s.value;
        return 0;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onSettingChanged() {}

    protected static void reloadWorld() {
        if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload();
    }
}
