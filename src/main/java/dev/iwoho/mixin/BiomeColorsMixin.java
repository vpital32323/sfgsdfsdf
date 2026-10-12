package dev.iwoho.mixin;

import dev.iwoho.Module;
import dev.iwoho.ModuleManager;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private static void iwoho$grass(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Module m = ModuleManager.GROUND;
        if (m != null && m.enabled) cir.setReturnValue(0xFF000000 | ModuleManager.color(m));
    }

    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void iwoho$foliage(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Module m = ModuleManager.GROUND;
        if (m != null && m.enabled) cir.setReturnValue(0xFF000000 | ModuleManager.color(m));
    }
}
