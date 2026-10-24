package net.zubrcraft.auth.client;

import net.minecraft.client.Minecraft;
import net.zubrcraft.auth.network.AuthPromptPacket;
import net.zubrcraft.auth.network.AuthResultPacket;

public class ClientHandler {
    private static volatile boolean pendingAuth = false;
    private static int lastMode = 0;
    private static String lastMessage = "";
    private static AuthScreen currentScreen = null;

    public static boolean isPendingAuth() {
        return pendingAuth;
    }

    public static void reset() {
        pendingAuth = false;
        currentScreen = null;
        lastMode = 0;
        lastMessage = "";
    }

    public static AuthScreen getOrCreateScreen() {
        if (currentScreen == null) {
            currentScreen = new AuthScreen(lastMode, lastMessage);
        }
        return currentScreen;
    }

    public static void handlePrompt(AuthPromptPacket packet) {
        lastMode = packet.mode;
        lastMessage = packet.message;
        pendingAuth = true;

        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91080_ instanceof AuthScreen screen) {
            currentScreen = screen;
            screen.updatePrompt(packet.mode, packet.message);
        } else {
            if (currentScreen == null) {
                currentScreen = new AuthScreen(packet.mode, packet.message);
            } else {
                currentScreen.updatePrompt(packet.mode, packet.message);
            }
            mc.m_91152_(currentScreen);
        }
    }

    public static void handleResult(AuthResultPacket packet) {
        Minecraft mc = Minecraft.m_91087_();
        if (packet.success) {
            pendingAuth = false;
            currentScreen = null;
            if (mc.f_91080_ instanceof AuthScreen) {
                mc.m_91152_(null);
            }
        } else {
            lastMessage = packet.message;
            if (mc.f_91080_ instanceof AuthScreen screen) {
                screen.setStatusMessage(packet.message);
            } else if (currentScreen != null) {
                currentScreen.setStatusMessage(packet.message);
            }
        }
    }
}
