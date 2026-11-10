package com.snolred.wildplus.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 내 컴퓨터에만 적용되는 설정 (config/wildplus-client.toml) */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_HUD = BUILDER
            .comment("왼쪽 위에 날짜·시각·좌표·방향 표시")
            .translation("wildplus.configuration.showHud")
            .define("showHud", true);

    public static final ModConfigSpec.BooleanValue HUD_COORDS = BUILDER
            .comment("HUD 에 좌표 표시")
            .translation("wildplus.configuration.hudCoords")
            .define("hudCoords", true);

    public static final ModConfigSpec.BooleanValue HEALTH_BARS = BUILDER
            .comment("몬스터 머리 위에 체력바 표시")
            .translation("wildplus.configuration.healthBars")
            .define("healthBars", true);

    public static final ModConfigSpec.BooleanValue MINIMAP = BUILDER
            .comment("오른쪽 위에 미니맵 표시")
            .translation("wildplus.configuration.minimap")
            .define("minimap", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {}
}
