package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 5.3.5 내 곡 올리기: 틱마다 조금씩 보냄 */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class MusicUploadTick {
    private MusicUploadTick() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        MusicClient.tickUpload();
    }
}
