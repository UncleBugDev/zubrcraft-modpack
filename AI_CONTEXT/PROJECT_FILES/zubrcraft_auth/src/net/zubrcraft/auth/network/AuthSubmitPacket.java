package net.zubrcraft.auth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.zubrcraft.auth.server.AuthManager;
import java.util.function.Supplier;

public class AuthSubmitPacket {
    public final int mode; // 0 = REGISTER, 1 = LOGIN
    public final String password;
    public final String confirmPassword;

    public AuthSubmitPacket(int mode, String password, String confirmPassword) {
        this.mode = mode;
        this.password = password != null ? password : "";
        this.confirmPassword = confirmPassword != null ? confirmPassword : "";
    }

    public AuthSubmitPacket(FriendlyByteBuf buf) {
        this.mode = buf.readInt();
        this.password = buf.m_130136_(128);
        this.confirmPassword = buf.m_130136_(128);
    }

    public static void encode(AuthSubmitPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.mode);
        buf.m_130072_(msg.password, 128);
        buf.m_130072_(msg.confirmPassword, 128);
    }

    public static void handle(AuthSubmitPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                AuthManager.getInstance().handleAuthSubmit(sender, msg.mode, msg.password, msg.confirmPassword);
            }
        });
        ctx.setPacketHandled(true);
    }
}
