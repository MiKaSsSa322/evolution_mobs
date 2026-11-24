package com.evolvingmobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(EvolvingMobs.MODID)
public class EvolvingMobs {
    public static final String MODID = "evolvingmobs";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"), () -> "1", "1"::equals, "1"::equals);

    public EvolvingMobs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CHANNEL.registerMessage(0, StagePacket.class, StagePacket::encode, StagePacket::decode, StagePacket::handle);
    }
}
