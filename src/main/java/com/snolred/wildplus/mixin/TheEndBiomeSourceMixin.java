package com.snolred.wildplus.mixin;

import com.snolred.wildplus.event.VoidBiome;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 엔드 바이옴 배치는 코드에 고정돼 있어서 결과를 바꿔 끼운다 */
@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {
    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true, require = 0)
    private void wildplus$possible(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        Holder<Biome> h = VoidBiome.holder;
        if (h != null) cir.setReturnValue(Stream.concat(cir.getReturnValue(), Stream.of(h)));
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;", at = @At("RETURN"), cancellable = true, require = 0)
    private void wildplus$biome(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> h = VoidBiome.holder;
        if (h == null) return;
        Holder<Biome> r = cir.getReturnValue();
        if ((r.is(Biomes.END_HIGHLANDS) || r.is(Biomes.END_MIDLANDS)) && VoidBiome.inZone(x, z)) cir.setReturnValue(h);
    }
}
