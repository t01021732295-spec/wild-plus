package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;

/** 4.1 입출금 내역: 사람별 최근 50건. "일차|내용|금액|잔액" 문자열로 저장 */
public final class BankLog {
    private BankLog() {}

    private static final int MAX = 50;

    public static void add(MinecraftServer server, UUID id, String label, long delta, long balance) {
        ContentData d = ContentData.get(server);
        CompoundTag t = d.player("bank_log", id);
        ListTag list = t.getListOrEmpty("l");
        list.add(0, StringTag.valueOf(ContentData.gameDay + "|" + label + "|" + delta + "|" + balance));
        while (list.size() > MAX) list.remove(list.size() - 1);
        t.put("l", list);
        d.dirty();
    }

    public static List<String> lines(MinecraftServer server, UUID id) {
        ListTag list = ContentData.get(server).player("bank_log", id).getListOrEmpty("l");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getStringOr(i, ""));
        return out;
    }
}
