package com.snolred.wildplus.content;

import com.mojang.serialization.Codec;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * 주말 콘텐츠 저장소 (world/data/wildplus_content.dat): 레이드 · 하늘의 탑 · 주간 목표 · 시즌 패스.
 * 형식이 자주 바뀌어도 안전하도록 CompoundTag 하나를 그대로 들고 다닌다.
 */
public class ContentData extends SavedData {
    public static final Codec<ContentData> CODEC = ExtraCodecs.converter(NbtOps.INSTANCE)
            .xmap(t -> new ContentData(t instanceof CompoundTag c ? c.copy() : new CompoundTag()), d -> d.root.copy());
    public static final SavedDataType<ContentData> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath("wildplus", "wildplus_content"), ContentData::new, CODEC);

    public final CompoundTag root;

    public ContentData() {
        this(new CompoundTag());
    }

    private ContentData(CompoundTag root) {
        this.root = root;
    }

    public static ContentData get(MinecraftServer server) {
        updateDay(server);
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public CompoundTag section(String name) {
        if (!(root.get(name) instanceof CompoundTag c)) {
            CompoundTag c2 = new CompoundTag();
            root.put(name, c2);
            return c2;
        }
        return c;
    }

    public CompoundTag player(String section, UUID id) {
        CompoundTag s = section(section);
        String k = id.toString();
        if (!(s.get(k) instanceof CompoundTag c)) {
            CompoundTag c2 = new CompoundTag();
            s.put(k, c2);
            return c2;
        }
        return c;
    }

    public void dirty() {
        setDirty();
    }

    /** 게임 속 날짜 (서버 틱마다 갱신) */
    public static volatile long gameDay;
    public static final int WEEK_DAYS = 7, SEASON_WEEKS = 8;

    public static void updateDay(MinecraftServer server) {
        gameDay = Math.max(0, server.overworld().getOverworldClockTime() / 24000L);
    }

    /** 게임 시간 기준 "주" 번호: 게임 속 7일마다 바뀐다 */
    public static long week() {
        return Math.floorDiv(gameDay, WEEK_DAYS);
    }

    /** 시즌 = 8주 (게임 속 56일) */
    public static long season() {
        return Math.floorDiv(week(), SEASON_WEEKS);
    }

    public static int weeksLeftInSeason() {
        return (int) (SEASON_WEEKS - Math.floorMod(week(), SEASON_WEEKS));
    }

    /** 다음 주까지 남은 게임 속 날 수 */
    public static int daysToReset() {
        return (int) (WEEK_DAYS - Math.floorMod(gameDay, WEEK_DAYS));
    }
}
