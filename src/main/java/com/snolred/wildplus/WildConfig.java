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
        BUILDER.push("convenience");
    }

    public static final ModConfigSpec.BooleanValue TREE_FELLING = BUILDER
            .comment("웅크린 채 도끼로 원목을 캐면 나무 전체를 벤다")
            .translation("wildplus.configuration.treeFelling")
            .define("treeFelling", true);

    public static final ModConfigSpec.BooleanValue VEIN_MINING = BUILDER
            .comment("웅크린 채 곡괭이로 광석을 캐면 이어진 광맥 전체를 캔다")
            .translation("wildplus.configuration.veinMining")
            .define("veinMining", true);

    public static final ModConfigSpec.BooleanValue RIGHT_CLICK_HARVEST = BUILDER
            .comment("다 자란 작물을 우클릭하면 수확하고 다시 심는다")
            .translation("wildplus.configuration.rightClickHarvest")
            .define("rightClickHarvest", true);

    public static final ModConfigSpec.BooleanValue GRAVES = BUILDER
            .comment("죽으면 아이템을 무덤에 보관한다")
            .translation("wildplus.configuration.graves")
            .define("graves", true);

    static {
        BUILDER.pop();
        BUILDER.push("economy");
    }

    public static final ModConfigSpec.IntValue GACHA_PRICE = BUILDER
            .comment("뽑기 기계 1회 가격 (동)")
            .translation("wildplus.configuration.gachaPrice")
            .defineInRange("gachaPrice", 10, 1, 10000);

    public static final ModConfigSpec.BooleanValue TAX_ENABLED = BUILDER
            .comment("소득세 (하루 소득 누진 5%~30%)")
            .translation("wildplus.configuration.taxEnabled")
            .define("taxEnabled", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WildConfig() {}
}
