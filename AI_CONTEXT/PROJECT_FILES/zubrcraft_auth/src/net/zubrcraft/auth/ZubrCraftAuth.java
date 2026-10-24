package net.zubrcraft.auth;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.zubrcraft.auth.network.AuthLeavePacket;
import net.zubrcraft.auth.network.AuthPromptPacket;
import net.zubrcraft.auth.network.AuthResultPacket;
import net.zubrcraft.auth.network.AuthSubmitPacket;
import net.zubrcraft.auth.server.ServerAuthEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ZubrCraftAuth.MODID)
public class ZubrCraftAuth {
    public static final String MODID = "zubrcraft_auth";
    public static final Logger LOGGER = LogManager.getLogger("ZubrCraftAuth");
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public ZubrCraftAuth() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(new ServerAuthEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new net.zubrcraft.auth.client.ClientAuthEvents()));
        LOGGER.info("ZubrCraft Auth mod initialized!");
    }

    private void setup(final FMLCommonSetupEvent event) {
        int id = 0;
        CHANNEL.registerMessage(id++, AuthPromptPacket.class, AuthPromptPacket::encode, AuthPromptPacket::new, AuthPromptPacket::handle);
        CHANNEL.registerMessage(id++, AuthSubmitPacket.class, AuthSubmitPacket::encode, AuthSubmitPacket::new, AuthSubmitPacket::handle);
        CHANNEL.registerMessage(id++, AuthResultPacket.class, AuthResultPacket::encode, AuthResultPacket::new, AuthResultPacket::handle);
        CHANNEL.registerMessage(id++, AuthLeavePacket.class, AuthLeavePacket::encode, AuthLeavePacket::new, AuthLeavePacket::handle);
        LOGGER.info("ZubrCraft Auth network packets registered.");
    }
}
