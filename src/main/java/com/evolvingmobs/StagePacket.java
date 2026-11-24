package com.evolvingmobs;

import com.evolvingmobs.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StagePacket(int id, int stage) {
    public static void encode(StagePacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.id);
        b.writeByte(p.stage);
    }

    public static StagePacket decode(FriendlyByteBuf b) {
        return new StagePacket(b.readVarInt(), b.readByte());
    }

    public static void handle(StagePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEvents.apply(p.id, p.stage)));
        ctx.get().setPacketHandled(true);
    }
}
