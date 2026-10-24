package net.zubrcraft.auth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class AuthResultPacket {
    public final boolean success;
    public final String message;

    public AuthResultPacket(boolean success, String message) {
        this.success = success;
        this.message = message != null ? message : "";
    }

    public AuthResultPacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.message = buf.m_130136_(256);
    }

    public static void encode(AuthResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.m_130072_(msg.message, 256);
    }

    public static void handle(AuthResultPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.zubrcraft.auth.client.ClientHandler.handleResult(msg);
            });
        });
        ctx.setPacketHandled(true);
    }
}
