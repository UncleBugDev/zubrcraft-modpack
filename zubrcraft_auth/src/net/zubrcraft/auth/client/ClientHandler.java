package net.zubrcraft.auth.client;

import net.minecraft.client.Minecraft;
import net.zubrcraft.auth.network.AuthPromptPacket;
import net.zubrcraft.auth.network.AuthResultPacket;

public class ClientHandler {
    public static void handlePrompt(AuthPromptPacket packet) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91080_ instanceof AuthScreen screen) {
            screen.updatePrompt(packet.mode, packet.message);
        } else {
            mc.m_91152_(new AuthScreen(packet.mode, packet.message));
        }
    }

    public static void handleResult(AuthResultPacket packet) {
        Minecraft mc = Minecraft.m_91087_();
        if (packet.success) {
            if (mc.f_91080_ instanceof AuthScreen) {
                mc.m_91152_(null);
            }
        } else {
            if (mc.f_91080_ instanceof AuthScreen screen) {
                screen.setStatusMessage(packet.message);
            }
        }
    }
}
