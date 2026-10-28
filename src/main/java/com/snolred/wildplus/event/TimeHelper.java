package com.snolred.wildplus.event;

import net.minecraft.server.level.ServerLevel;

/** 월드 시각(0~24000 주기) 조회를 한 곳에 모아 둔다 */
final class TimeHelper {
    private TimeHelper() {}

    static long dayTime(ServerLevel level) {
        return level.getOverworldClockTime();
    }
}
