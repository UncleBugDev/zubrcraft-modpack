package net.zubrcraft.auth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class AuthPromptPacket {
    public final int mode; // 0 = REGISTER, 1 = LOGIN
    public final String message;

    public AuthPromptPacket(int mode, String message) {
        this.mode = mode;
        this.message = message != null ? message : "";
    }

    public AuthPromptPacket(FriendlyByteBuf buf) {
        this.mode = buf.readInt();
        this.message = buf.m_130136_(256);
    }

    public static void encode(AuthPromptPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.mode);
        buf.m_130072_(msg.message, 256);
    }

    public static void handle(AuthPromptPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.zubrcraft.auth.client.ClientHandler.handlePrompt(msg);
            });
        });
        ctx.setPacketHandled(true);
    }
}
