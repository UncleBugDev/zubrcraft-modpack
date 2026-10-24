package net.zubrcraft.auth.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

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
                UUID uuid = player.m_20148_();

                // 1. Timeout protection: 60 seconds to authenticate or kicked
                long joinTime = AuthManager.getInstance().getJoinTime(uuid);
                if (joinTime > 0 && System.currentTimeMillis() - joinTime > 60000L) {
                    player.f_8906_.m_9942_(Component.m_237113_("§c[ZubrCraft] Время на авторизацию истекло (60 сек)!"));
                    return;
                }

                // 2. Freeze physics & prevent falling/void damage
                player.m_20256_(Vec3.f_82478_); // ZERO velocity
                player.f_19789_ = 0.0f; // fallDistance = 0

                // 3. Freeze position (teleport back to initial join location if moved)
                Vec3 joinPos = AuthManager.getInstance().getJoinPosition(uuid);
                if (joinPos != null) {
                    double distSq = player.m_20275_(joinPos.f_82479_, joinPos.f_82480_, joinPos.f_82481_);
                    if (distSq > 0.04) {
                        float yaw = AuthManager.getInstance().getJoinYaw(uuid);
                        float pitch = AuthManager.getInstance().getJoinPitch(uuid);
                        player.f_8906_.m_9774_(joinPos.f_82479_, joinPos.f_82480_, joinPos.f_82481_, yaw, pitch);
                    }
                }

                // 4. Blindness to prevent inspecting surroundings / reading coords before logging in
                if (!player.m_21023_(MobEffects.f_19610_)) {
                    player.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 20 * 120, 1, false, false, false));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
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
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
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
    public void onCommand(CommandEvent event) {
        if (event.getParseResults() != null && event.getParseResults().getContext() != null) {
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            if (source != null && source.m_81373_() instanceof ServerPlayer player) {
                if (!AuthManager.getInstance().isAuthenticated(player)) {
                    event.setCanceled(true);
                    player.m_213846_(Component.m_237113_("§c[ZubrCraft] Вы не можете использовать команды до авторизации!"));
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
            player.m_213846_(Component.m_237113_("§c[ZubrCraft] Вы не можете писать в чат до авторизации!"));
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !AuthManager.getInstance().isAuthenticated(player)) {
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
