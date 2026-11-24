package com.evolvingmobs.client;

import com.evolvingmobs.ClientStages;
import com.evolvingmobs.Evo;
import com.evolvingmobs.EvolvingMobs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EvolvingMobs.MODID, value = Dist.CLIENT)
public class ClientEvents {

    public static void apply(int id, int stage) {
        ClientStages.put(id, stage);
        Level lv = Minecraft.getInstance().level;
        if (lv != null) {
            Entity e = lv.getEntity(id);
            if (e != null) e.refreshDimensions();
        }
    }

    /** Визуал: увеличение модели, а у стадии 3 ещё и перекос + «дёргающееся» тело. */
    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity le = e.getEntity();
        if (!Evo.eligible(le)) return;
        int s = ClientStages.get(le.getId());
        if (s <= 0) return;
        PoseStack ps = e.getPoseStack();
        float w = Evo.W[s], h = Evo.H[s];
        if (s >= 3) {
            float t = le.tickCount + e.getPartialTick();
            ps.mulPose(Axis.ZP.rotationDegrees((le.getId() & 1) == 0 ? 7f : -7f));
            h *= 1f + 0.04f * (float) Math.sin(t * 0.35f);
            w *= 1f - 0.03f * (float) Math.sin(t * 0.35f);
        }
        ps.scale(w, h, w);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { ClientStages.clear(); }
}
