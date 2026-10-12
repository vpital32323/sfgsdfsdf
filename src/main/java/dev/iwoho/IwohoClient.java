package dev.iwoho;

import dev.iwoho.gui.ClickGui;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class IwohoClient implements ClientModInitializer {
    public static final String NAME = "iwoho client";
    /** Цвет темы GUI (оттенок 0..360) — меняется в панели Theme */
    public static final Module.Setting THEME_HUE = new Module.Setting("Hue", 0, 360, 265);
    private static KeyBinding openKey;

    public static int accent() {
        return MathHelper.hsvToRgb((float) THEME_HUE.value / 360f, 0.62f, 1f) & 0xFFFFFF;
    }

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iwoho.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.iwoho"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) mc.setScreen(new ClickGui());
            if (mc.player == null || mc.world == null) return;
            for (Module m : ModuleManager.ALL) if (m.enabled) m.onTick();
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden) return;
            var tr = mc.textRenderer;
            int acc = 0xFF000000 | accent();
            ctx.drawTextWithShadow(tr, NAME, 6, 6, acc);
            List<Module> on = ModuleManager.ALL.stream().filter(m -> m.enabled)
                    .sorted(Comparator.comparingInt((Module m) -> -tr.getWidth(m.name))).toList();
            int y = 6;
            for (Module m : on) {
                ctx.drawTextWithShadow(tr, m.name, ctx.getScaledWindowWidth() - tr.getWidth(m.name) - 6, y, acc);
                y += 10;
            }
        });
    }
}
