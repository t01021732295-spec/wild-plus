package com.snolred.wildplus.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;

/** 황금 슬라임: 지하 깊은 곳에 드물게 나타나고, 잡으면 동전을 쏟는다. (BanditEvents.onDrops) */
public class GoldenSlimeEntity extends Slime {
    public GoldenSlimeEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }
}
