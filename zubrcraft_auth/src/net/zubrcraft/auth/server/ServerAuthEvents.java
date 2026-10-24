package net.zubrcraft.auth.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerAuthEvents {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AuthManager.getInstance().onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AuthManager.getInstance().removePlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            if (!AuthManager.getInstance().isAuthenticated(player)) {
                player.m_20256_(new Vec3(0, 0, 0));
                player.f_19789_ = 0.0f;
                if (player.f_19797_ % 40 == 0) {
                    AuthManager.getInstance().checkPlayerScreen(player);
                }
            }
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!AuthManager.getInstance().isAuthenticated(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().m_7639_() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().m_7639_() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
            player.m_213846_(Component.m_237113_("§cСначала авторизуйтесь через появившееся окно!"));
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            player.m_6915_();
        }
    }
}
