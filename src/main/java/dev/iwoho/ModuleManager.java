package dev.iwoho;

import dev.iwoho.Module.Category;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    public static Module SKY, GROUND;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static void reg(Module m) { ALL.add(m); }

    /** HSB-настройки модуля -> RGB */
    public static int color(Module m) {
        return MathHelper.hsvToRgb((float) m.get("Hue") / 360f,
                (float) m.get("Saturation") / 100f,
                (float) m.get("Brightness") / 100f) & 0xFFFFFF;
    }

    public static void init() {
        // ───────── COMBAT ─────────
        reg(new Module("Kill Aura", Category.COMBAT) {
            { add("Range", 2, 6, 4); }
            @Override public void onTick() {
                ClientPlayerEntity p = mc.player;
                if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;
                if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
                double r = get("Range");
                double best = r * r;
                LivingEntity target = null;
                for (Entity e : mc.world.getEntities()) {
                    if (!(e instanceof PlayerEntity pe) || pe == p || !pe.isAlive()) continue;
                    double d = p.squaredDistanceTo(pe);
                    if (d < best) { best = d; target = pe; }
                }
                if (target != null) {
                    mc.interactionManager.attackEntity(p, target);
                    p.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        reg(new Module("Auto Totem", Category.COMBAT) {
            private int delay;
            @Override public void onTick() {
                ClientPlayerEntity p = mc.player;
                if (p == null || mc.interactionManager == null) return;
                if (delay > 0) { delay--; return; }
                if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
                for (int i = 0; i < 36; i++) {
                    if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                        int slot = i < 9 ? i + 36 : i;
                        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40,
                                SlotActionType.SWAP, p);
                        delay = 3;
                        return;
                    }
                }
            }
        });

        // ───────── MOVEMENT ─────────
        reg(new Module("Auto Sprint", Category.MOVEMENT) {
            @Override public void onTick() {
                ClientPlayerEntity p = mc.player;
                if (p != null && p.input.movementForward > 0 && !p.horizontalCollision && !p.isSneaking())
                    p.setSprinting(true);
            }
        });

        reg(new Module("Flight", Category.MOVEMENT) {
            { add("Speed", 0.5, 5, 1.5); }
            @Override public void onTick() {
                PlayerAbilities a = mc.player.getAbilities();
                a.allowFlying = true;
                a.flying = true;
                a.setFlySpeed((float) (get("Speed") * 0.05));
            }
            @Override public void onDisable() {
                if (mc.player == null) return;
                PlayerAbilities a = mc.player.getAbilities();
                a.setFlySpeed(0.05f);
                if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                    a.flying = false;
                    a.allowFlying = false;
                }
            }
        });

        // ───────── RENDER ─────────
        reg(new Module("Full Bright", Category.RENDER) {
            @Override public void onTick() {
                var p = mc.player;
                var eff = p.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (eff == null || eff.getDuration() < 240)
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false, false));
            }
            @Override public void onDisable() {
                if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        // ───────── WORLD (косметика) ─────────
        SKY = new Module("Sky Color", Category.WORLD) {
            { add("Hue", 0, 360, 280); add("Saturation", 0, 100, 60); add("Brightness", 0, 100, 90); }
        };
        reg(SKY);

        GROUND = new Module("Ground Color", Category.WORLD) {
            { add("Hue", 0, 360, 300); add("Saturation", 0, 100, 55); add("Brightness", 0, 100, 85); }
            @Override public void onEnable() { reloadWorld(); }
            @Override public void onDisable() { reloadWorld(); }
            @Override public void onSettingChanged() { if (enabled) reloadWorld(); }
        };
        reg(GROUND);

        reg(new Module("Time Changer", Category.WORLD) {
            { add("Time", 0, 24, 18); } // x1000 тиков: 0 рассвет, 6 день, 12 закат, 18 полночь
            @Override public void onTick() {
                mc.world.getLevelProperties().setTimeOfDay((long) (get("Time") * 1000));
            }
        });
    }
}
