package dev.iwoho.mixin;

import dev.iwoho.Module;
import dev.iwoho.ModuleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void iwoho$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        Module m = ModuleManager.SKY;
        if (m != null && m.enabled) cir.setReturnValue(0xFF000000 | ModuleManager.color(m));
    }
}
