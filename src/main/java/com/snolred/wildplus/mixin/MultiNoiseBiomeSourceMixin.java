package com.snolred.wildplus.mixin;

import com.snolred.wildplus.event.NetherBiomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 5.1.1 네더 바이옴 2종: 바닐라가 고른 결과를 바꿔 끼운다 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Inject(method = "getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;", at = @At("RETURN"), cancellable = true, require = 0)
    private void wildplus$nether(Climate.TargetPoint t, CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> r = cir.getReturnValue();
        Holder<Biome> s = NetherBiomes.swap(r, t);
        if (s != r) cir.setReturnValue(s);
    }
}
