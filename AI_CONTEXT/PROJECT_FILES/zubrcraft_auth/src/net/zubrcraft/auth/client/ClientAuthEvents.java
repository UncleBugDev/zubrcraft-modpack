package net.zubrcraft.auth.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientAuthEvents {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Apply custom window and taskbar icon
            WindowIconHelper.applyIcon();

            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91074_ != null && mc.f_91073_ != null) { // local player and level exist
                if (ClientHandler.isPendingAuth()) {
                    if (!(mc.f_91080_ instanceof AuthScreen)) {
                        mc.m_91152_(ClientHandler.getOrCreateScreen());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHandler.reset();
    }
}
