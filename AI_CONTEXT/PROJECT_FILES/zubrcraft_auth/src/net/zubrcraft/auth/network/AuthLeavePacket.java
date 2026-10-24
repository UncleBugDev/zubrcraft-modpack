package net.zubrcraft.auth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class AuthLeavePacket {
    public AuthLeavePacket() {}

    public AuthLeavePacket(FriendlyByteBuf buf) {}

    public static void encode(AuthLeavePacket msg, FriendlyByteBuf buf) {}

    public static void handle(AuthLeavePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                sender.f_8906_.m_9942_(Component.m_237113_("Вы вышли с сервера."));
            }
        });
        ctx.setPacketHandled(true);
    }
}
