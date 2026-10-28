package com.snolred.wildplus;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 서버 설정 (월드 폴더의 serverconfig/wildplus-server.toml) */
public final class WildConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("bloodMoon");
    }

    public static final ModConfigSpec.DoubleValue BLOOD_MOON_CHANCE = BUILDER
            .comment("밤이 시작될 때 블러드문이 뜰 확률 (0.0 ~ 1.0)")
            .translation("wildplus.configuration.bloodMoonChance")
            .defineInRange("bloodMoonChance", 0.12, 0.0, 1.0);

    public static final ModConfigSpec.IntValue BLOOD_MOON_GRACE_DAYS = BUILDER
            .comment("월드 생성 후 블러드문이 뜨지 않는 날 수")
            .translation("wildplus.configuration.bloodMoonGraceDays")
            .defineInRange("bloodMoonGraceDays", 3, 0, 1000);

    public static final ModConfigSpec.IntValue BLOOD_MOON_EXTRA_SPAWNS = BUILDER
            .comment("블러드문 동안 플레이어 주변에 10초마다 추가로 소환되는 몹 수 (0이면 끔)")
            .translation("wildplus.configuration.bloodMoonExtraSpawns")
            .defineInRange("bloodMoonExtraSpawns", 2, 0, 8);

    static {
        BUILDER.pop();
        BUILDER.push("eliteMobs");
    }

    public static final ModConfigSpec.DoubleValue ELITE_CHANCE = BUILDER
            .comment("자연 스폰된 적대 몹이 정예 몹이 될 확률 (블러드문 중에는 3배)")
            .translation("wildplus.configuration.eliteChance")
            .defineInRange("eliteChance", 0.05, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WildConfig() {}
}
