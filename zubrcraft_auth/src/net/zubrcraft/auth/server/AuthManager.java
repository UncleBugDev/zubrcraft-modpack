package net.zubrcraft.auth.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.zubrcraft.auth.ZubrCraftAuth;
import net.zubrcraft.auth.network.AuthPromptPacket;
import net.zubrcraft.auth.network.AuthResultPacket;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private static final AuthManager INSTANCE = new AuthManager();
    public static AuthManager getInstance() { return INSTANCE; }

    public static class UserData {
        public String hash;
        public String salt;
        public String lastIp;
        public long registeredAt;
        public long lastLogin;
    }

    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPromptSent = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile = new File("config/zubrcraft_auth/users.json");

    private AuthManager() {
        loadUsers();
    }

    public synchronized void loadUsers() {
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                saveUsers();
                return;
            }
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, UserData>>(){}.getType();
                Map<String, UserData> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    users.clear();
                    users.putAll(loaded);
                }
            }
            ZubrCraftAuth.LOGGER.info("Loaded " + users.size() + " registered users.");
        } catch (Exception e) {
            ZubrCraftAuth.LOGGER.error("Failed to load users from " + configFile, e);
        }
    }

    public synchronized void saveUsers() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(users, writer);
            }
        } catch (Exception e) {
            ZubrCraftAuth.LOGGER.error("Failed to save users to " + configFile, e);
        }
    }

    public boolean isAuthenticated(ServerPlayer player) {
        if (player == null) return false;
        return authenticatedPlayers.contains(player.m_20148_());
    }

    public void removePlayer(ServerPlayer player) {
        if (player != null) {
            authenticatedPlayers.remove(player.m_20148_());
            attempts.remove(player.m_20148_());
            lastPromptSent.remove(player.m_20148_());
        }
    }

    public String getPlayerIP(ServerPlayer player) {
        try {
            if (player.f_8906_ != null && player.f_8906_.f_9742_ != null) {
                SocketAddress addr = player.f_8906_.f_9742_.m_129523_();
                if (addr instanceof InetSocketAddress inet) {
                    if (inet.getAddress() != null) {
                        return inet.getAddress().getHostAddress();
                    }
                    return inet.getHostString();
                } else if (addr != null) {
                    String s = addr.toString();
                    if (s.startsWith("/")) s = s.substring(1);
                    int colon = s.indexOf(':');
                    return colon > 0 ? s.substring(0, colon) : s;
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    public void onPlayerJoin(ServerPlayer player) {
        String username = player.m_36316_().getName().toLowerCase(Locale.ROOT);
        UserData user = users.get(username);
        String currentIp = getPlayerIP(player);
        UUID uuid = player.m_20148_();

        if (user == null) {
            authenticatedPlayers.remove(uuid);
            sendPrompt(player, 0, "");
            player.m_213846_(Component.m_237113_("§e[ZubrCraft] Пожалуйста, зарегистрируйтесь через появившееся окно!"));
        } else {
            if (currentIp.equals(user.lastIp)) {
                // Same IP: Seamless automatic background authentication!
                authenticatedPlayers.add(uuid);
                user.lastLogin = System.currentTimeMillis();
                saveUsers();
                sendResult(player, true, "Авторизация успешна!");
                player.m_213846_(Component.m_237113_("§a[ZubrCraft] С возвращением, " + player.m_36316_().getName() + "! Вы успешно авторизованы (по IP)."));
            } else {
                // Different IP: Prompt for password via GUI
                authenticatedPlayers.remove(uuid);
                sendPrompt(player, 1, "Вход с нового IP адреса (" + currentIp + ")");
                player.m_213846_(Component.m_237113_("§e[ZubrCraft] Обнаружен вход с нового IP (" + currentIp + "). Пожалуйста, введите пароль."));
            }
        }
    }

    public void handleAuthSubmit(ServerPlayer player, int mode, String password, String confirm) {
        String username = player.m_36316_().getName().toLowerCase(Locale.ROOT);
        String currentIp = getPlayerIP(player);
        UserData user = users.get(username);
        UUID uuid = player.m_20148_();

        if (mode == 0) { // REGISTER
            if (user != null) {
                sendPrompt(player, 1, "§cАккаунт уже зарегистрирован! Введите пароль.");
                return;
            }
            if (password == null || password.trim().length() < 4) {
                sendPrompt(player, 0, "§cПароль должен быть не менее 4 символов!");
                return;
            }
            if (!password.equals(confirm)) {
                sendPrompt(player, 0, "§cПароли не совпадают!");
                return;
            }

            user = new UserData();
            user.salt = generateSalt();
            user.hash = hashPassword(password, user.salt);
            user.lastIp = currentIp;
            user.registeredAt = System.currentTimeMillis();
            user.lastLogin = user.registeredAt;
            users.put(username, user);
            saveUsers();

            authenticatedPlayers.add(uuid);
            sendResult(player, true, "Регистрация успешна!");
            player.m_213846_(Component.m_237113_("§a[ZubrCraft] Регистрация успешна! Добро пожаловать, " + player.m_36316_().getName() + "!"));
        } else if (mode == 1) { // LOGIN
            if (user == null) {
                sendPrompt(player, 0, "§cАккаунт не найден! Пожалуйста, зарегистрируйтесь.");
                return;
            }

            String computed = hashPassword(password, user.salt);
            if (computed.equals(user.hash)) {
                user.lastIp = currentIp;
                user.lastLogin = System.currentTimeMillis();
                saveUsers();

                authenticatedPlayers.add(uuid);
                attempts.remove(uuid);
                sendResult(player, true, "Авторизация успешна!");
                player.m_213846_(Component.m_237113_("§a[ZubrCraft] Вы успешно вошли! Приятной игры."));
            } else {
                int count = attempts.getOrDefault(uuid, 0) + 1;
                attempts.put(uuid, count);
                if (count >= 5) {
                    player.f_8906_.m_9942_(Component.m_237113_("§cПревышено количество попыток ввода пароля."));
                } else {
                    sendPrompt(player, 1, "§cНеверный пароль! Осталось попыток: " + (5 - count));
                }
            }
        }
    }

    public void checkPlayerScreen(ServerPlayer player) {
        if (!isAuthenticated(player)) {
            long now = System.currentTimeMillis();
            long last = lastPromptSent.getOrDefault(player.m_20148_(), 0L);
            if (now - last > 3000) { // every 3 seconds if not authenticated, ensure screen is open
                String username = player.m_36316_().getName().toLowerCase(Locale.ROOT);
                int mode = users.containsKey(username) ? 1 : 0;
                sendPrompt(player, mode, "");
            }
        }
    }

    public void sendPrompt(ServerPlayer player, int mode, String errorMsg) {
        lastPromptSent.put(player.m_20148_(), System.currentTimeMillis());
        ZubrCraftAuth.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AuthPromptPacket(mode, errorMsg));
    }

    public void sendResult(ServerPlayer player, boolean success, String message) {
        ZubrCraftAuth.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AuthResultPacket(success, message));
    }

    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }
}
