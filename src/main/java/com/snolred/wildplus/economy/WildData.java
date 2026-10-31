package com.snolred.wildplus.economy;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * 월드 전체에 저장되는 야생 플러스 데이터 (world/data/wildplus_world.dat).
 * 워프 포인트 목록, 은행 계좌, 주식 보유, 마지막 시세, 의뢰 완료 기록.
 */
public class WildData extends SavedData {
    public static final Codec<WildData> CODEC = ExtraCodecs.converter(NbtOps.INSTANCE).xmap(WildData::decode, WildData::encode);
    public static final SavedDataType<WildData> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath("wildplus", "wildplus_world"), WildData::new, CODEC);

    public static WildData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // ───────── 워프 ─────────
    public static final class Warp {
        public final UUID id;
        public String name;
        public final UUID owner;
        public String ownerName;
        public final String dimension;
        public final BlockPos pos;
        public boolean isPublic;

        public Warp(UUID id, String name, UUID owner, String ownerName, String dimension, BlockPos pos, boolean isPublic) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.ownerName = ownerName;
            this.dimension = dimension;
            this.pos = pos;
            this.isPublic = isPublic;
        }
    }

    public final Map<UUID, Warp> warps = new LinkedHashMap<>();

    // ───────── 은행 ─────────
    public final Map<UUID, Long> bank = new HashMap<>();
    public long lastInterestDay = -1;

    public long balance(UUID player) {
        return bank.getOrDefault(player, 0L);
    }

    public void addBalance(UUID player, long delta) {
        bank.put(player, Math.max(0L, balance(player) + delta));
        setDirty();
    }

    // ───────── 주식 ─────────
    /** 플레이어 → 종목 코드 → [보유 주식 수, 총 매입액] */
    public final Map<UUID, Map<String, long[]>> stocks = new HashMap<>();
    /** 마지막으로 받은 시세 (서버 재시작 후에도 유지): 코드 → [현재가(동×100), 전일가(동×100)] */
    public final Map<String, long[]> lastPrices = new HashMap<>();

    public long[] holding(UUID player, String code) {
        return stocks.computeIfAbsent(player, k -> new HashMap<>()).computeIfAbsent(code, k -> new long[2]);
    }

    // ───────── 의뢰 ─────────
    /** 플레이어 → [의뢰 날짜, 완료 비트] */
    public final Map<UUID, long[]> bounties = new HashMap<>();

    public int bountyDone(UUID player, long day) {
        long[] v = bounties.get(player);
        return v != null && v[0] == day ? (int) v[1] : 0;
    }

    public void markBountyDone(UUID player, long day, int index) {
        bounties.put(player, new long[]{day, bountyDone(player, day) | (1 << index)});
        setDirty();
    }

    // ───────── 세금 · 청크로더 ─────────
    /** 플레이어 → [날짜, 그날 소득, 그날 낸 세금] */
    public final Map<UUID, long[]> taxes = new HashMap<>();
    public long treasury;
    /** "차원|청크x|청크z" → 그 청크에 놓인 청크로더 수 */
    public final Map<String, Integer> chunkLoaders = new HashMap<>();

    // ───────── 명예의 전당 ─────────
    /** 명예 등급 (0 없음 ~ 5 레전드) */
    public final Map<UUID, Integer> honor = new HashMap<>();
    /** 비행 허가 만료 시각 (오버월드 게임 틱). 키가 있으면 이 모드가 비행을 준 것 */
    public final Map<UUID, Long> flightUntil = new HashMap<>();
    /** 마지막으로 본 이름 (순위표용) */
    public final Map<UUID, String> names = new HashMap<>();
    public long festivalUntil;
    public String festivalBy = "";

    public int honor(UUID player) {
        return honor.getOrDefault(player, 0);
    }

    // ───────── 저장 ─────────
    private CompoundTag encode() {
        CompoundTag root = new CompoundTag();
        ListTag warpList = new ListTag();
        for (Warp w : warps.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", w.id.toString());
            t.putString("name", w.name);
            t.putString("owner", w.owner.toString());
            t.putString("owner_name", w.ownerName);
            t.putString("dim", w.dimension);
            t.putLong("pos", w.pos.asLong());
            t.putBoolean("public", w.isPublic);
            warpList.add(t);
        }
        root.put("warps", warpList);

        CompoundTag bankTag = new CompoundTag();
        bank.forEach((k, v) -> bankTag.putLong(k.toString(), v));
        root.put("bank", bankTag);
        root.putLong("last_interest_day", lastInterestDay);

        CompoundTag stockTag = new CompoundTag();
        stocks.forEach((player, map) -> {
            CompoundTag p = new CompoundTag();
            map.forEach((code, v) -> {
                if (v[0] > 0) p.putLongArray(code, v.clone());
            });
            stockTag.put(player.toString(), p);
        });
        root.put("stocks", stockTag);
        CompoundTag priceTag = new CompoundTag();
        lastPrices.forEach((code, v) -> priceTag.putLongArray(code, v.clone()));
        root.put("prices", priceTag);

        CompoundTag bountyTag = new CompoundTag();
        bounties.forEach((k, v) -> bountyTag.putLongArray(k.toString(), v.clone()));
        root.put("bounties", bountyTag);
        CompoundTag taxTag = new CompoundTag();
        taxes.forEach((k, v) -> taxTag.putLongArray(k.toString(), v.clone()));
        root.put("taxes", taxTag);
        root.putLong("treasury", treasury);
        CompoundTag loaders = new CompoundTag();
        chunkLoaders.forEach((k, v) -> { if (v > 0) loaders.putInt(k, v); });
        root.put("chunk_loaders", loaders);
        CompoundTag honorTag = new CompoundTag();
        honor.forEach((k, v) -> honorTag.putInt(k.toString(), v));
        root.put("honor", honorTag);
        CompoundTag flightTag = new CompoundTag();
        flightUntil.forEach((k, v) -> flightTag.putLong(k.toString(), v));
        root.put("flight", flightTag);
        CompoundTag nameTag = new CompoundTag();
        names.forEach((k, v) -> nameTag.putString(k.toString(), v));
        root.put("names", nameTag);
        root.putLong("festival_until", festivalUntil);
        root.putString("festival_by", festivalBy);
        return root;
    }

    private static WildData decode(Tag tag) {
        WildData d = new WildData();
        if (!(tag instanceof CompoundTag root)) {
            return d;
        }
        for (Tag t0 : root.getListOrEmpty("warps")) {
            if (!(t0 instanceof CompoundTag t)) continue;
            try {
                Warp w = new Warp(UUID.fromString(t.getStringOr("id", "")), t.getStringOr("name", "?"),
                        UUID.fromString(t.getStringOr("owner", "")), t.getStringOr("owner_name", ""),
                        t.getStringOr("dim", "minecraft:overworld"), BlockPos.of(t.getLongOr("pos", 0L)), t.getBooleanOr("public", true));
                d.warps.put(w.id, w);
            } catch (IllegalArgumentException ignored) {
                // 손상된 항목은 건너뜀
            }
        }
        CompoundTag bankTag = root.getCompoundOrEmpty("bank");
        for (String k : bankTag.keySet()) {
            parse(k).ifPresent(u -> d.bank.put(u, bankTag.getLongOr(k, 0L)));
        }
        d.lastInterestDay = root.getLongOr("last_interest_day", -1L);
        CompoundTag stockTag = root.getCompoundOrEmpty("stocks");
        for (String k : stockTag.keySet()) {
            parse(k).ifPresent(u -> {
                CompoundTag p = stockTag.getCompoundOrEmpty(k);
                Map<String, long[]> map = new HashMap<>();
                for (String code : p.keySet()) {
                    p.getLongArray(code).ifPresent(arr -> {
                        if (arr.length >= 2) map.put(code, new long[]{arr[0], arr[1]});
                    });
                }
                d.stocks.put(u, map);
            });
        }
        CompoundTag priceTag = root.getCompoundOrEmpty("prices");
        for (String code : priceTag.keySet()) {
            priceTag.getLongArray(code).ifPresent(arr -> {
                if (arr.length >= 2) d.lastPrices.put(code, new long[]{arr[0], arr[1]});
            });
        }
        CompoundTag bountyTag = root.getCompoundOrEmpty("bounties");
        for (String k : bountyTag.keySet()) {
            parse(k).ifPresent(u -> bountyTag.getLongArray(k).ifPresent(arr -> {
                if (arr.length >= 2) d.bounties.put(u, new long[]{arr[0], arr[1]});
            }));
        }
        CompoundTag taxTag = root.getCompoundOrEmpty("taxes");
        for (String k : taxTag.keySet()) {
            parse(k).ifPresent(u -> taxTag.getLongArray(k).ifPresent(arr -> {
                if (arr.length >= 3) d.taxes.put(u, new long[]{arr[0], arr[1], arr[2]});
            }));
        }
        d.treasury = root.getLongOr("treasury", 0L);
        CompoundTag loaders = root.getCompoundOrEmpty("chunk_loaders");
        for (String k : loaders.keySet()) d.chunkLoaders.put(k, loaders.getIntOr(k, 0));
        CompoundTag honorTag = root.getCompoundOrEmpty("honor");
        for (String k : honorTag.keySet()) parse(k).ifPresent(u -> d.honor.put(u, honorTag.getIntOr(k, 0)));
        CompoundTag flightTag = root.getCompoundOrEmpty("flight");
        for (String k : flightTag.keySet()) parse(k).ifPresent(u -> d.flightUntil.put(u, flightTag.getLongOr(k, 0L)));
        CompoundTag nameTag = root.getCompoundOrEmpty("names");
        for (String k : nameTag.keySet()) parse(k).ifPresent(u -> d.names.put(u, nameTag.getStringOr(k, "?")));
        d.festivalUntil = root.getLongOr("festival_until", 0L);
        d.festivalBy = root.getStringOr("festival_by", "");
        return d;
    }

    private static java.util.Optional<UUID> parse(String s) {
        try {
            return java.util.Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    /** 워프 목록을 이름순으로 */
    public List<Warp> sortedWarps() {
        List<Warp> list = new ArrayList<>(warps.values());
        list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return list;
    }
}
