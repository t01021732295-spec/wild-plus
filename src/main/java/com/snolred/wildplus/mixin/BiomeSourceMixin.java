package com.snolred.wildplus.mixin;

import com.snolred.wildplus.event.VoidBiome;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 엔드 바이옴 목록에 평원을 더한다. 바닐라는 목록을 처음 한 번만 계산해 두는데, 새 세계 만들기 화면이
 * 서버 시작 전에 그 계산을 해 버리므로 (평원이 빠진 채 굳어 청크 생성이 터짐) 돌려줄 때마다 붙인다.
 */
@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin {
    @Unique private Holder<Biome> wildplus$for;
    @Unique private Set<Holder<Biome>> wildplus$set;

    @Inject(method = "possibleBiomes", at = @At("RETURN"), cancellable = true)
    private void wildplus$possible(CallbackInfoReturnable<Set<Holder<Biome>>> cir) {
        Holder<Biome> h;
        Holder<Biome> h2 = null;
        if ((Object) this instanceof TheEndBiomeSource) {
            h = VoidBiome.holder;
        } else if ((Object) this instanceof net.minecraft.world.level.biome.MultiNoiseBiomeSource) {
            // 5.1.1: 네더(황무지가 들어 있는 목록)에만 잿불 황야 · 재의 숲을 더한다
            h = com.snolred.wildplus.event.NetherBiomes.ember;
            h2 = com.snolred.wildplus.event.NetherBiomes.ash;
            if (h == null || h2 == null) return;
            boolean nether = false;
            for (Holder<Biome> b : cir.getReturnValue()) if (b.is(net.minecraft.world.level.biome.Biomes.NETHER_WASTES)) { nether = true; break; }
            if (!nether) return;
        } else {
            return;
        }
        if (h == null) return;
        if (wildplus$for != h) {
            Set<Holder<Biome>> s = new HashSet<>(cir.getReturnValue());
            s.add(h);
            if (h2 != null) s.add(h2);
            wildplus$set = s;
            wildplus$for = h;
        }
        cir.setReturnValue(wildplus$set);
    }
}
