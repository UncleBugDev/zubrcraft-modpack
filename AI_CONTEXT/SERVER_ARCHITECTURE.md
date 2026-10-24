# Архитектура и сетевая инфраструктура ZubrCraft

Этот файл содержит точные технические характеристики оборудования, сетей, портов и служб сервера ZubrCraft.

---

## 1. Серверные узлы и сети

### Физический сервер (`unclebug-server`):
- **ОС:** Ubuntu 24.04 LTS (x86_64).
- **Локальный IP:** `192.168.100.51` (домашняя сеть за роутером Белтелеком Huawei HG8245H-256M).
- **Белый WAN IP:** `86.57.216.9` (динамический / квази-статический, Белтелеком).
- **Tailscale Mesh IP:** `100.85.40.18` (для прямого SSH-доступа отовсюду).
- **SSH доступ:** `ssh unclebug@192.168.100.51` (или через Tailscale).

---

## 2. Маршрутизация портов и туннели

| Назначение | Протокол | Внешний адрес / Домен | Внутренний порт на сервере | Как работает |
|---|---|---|---|---|
| Основной вход в игру | TCP | `zubrcraft.fun:25565` | `25565` | Прямой проброс портов на роутере (Port Forwarding WAN `86.57.216.9` -> `192.168.100.51:25565`). DNS A-запись в Cloudflare (DNS-only, Grey Cloud). |
| Резервный вход (VPN) | TCP | `vivo-whomever.tun.ply.gg:43020` | `25565` (через socat `25566`) | Anycast Playit.gg туннель (`playit.service`). Обходит блокировки европейских провайдеров и зарубежных VPN (Surfshark, NordVPN) к IP Белтелекома. Скрывает домашний IP. |
| Голосовой чат (Voice) | UDP | `audio-whomever.tun.ply.gg:33796` | `24454` | Simple Voice Chat через Playit.gg UDP туннель. На клиентах и сервере настроен порт `24454`. |
| CDN раздачи модов | HTTPS | `https://sync.zubrcraft.fun/` | `8085` | Nginx порт 8085 (`/home/unclebug/storage/zubrcraft-sync`), проброшен через `cloudflared.service` (Cloudflare Tunnel). Полная защита от DDoS и сверхбыстрая отдача через CDN кэш. |

---

## 3. Службы systemd на сервере

1. **`minecraft.service`**:
   - Запускает Forge-сервер 1.20.1 в GNU Screen: `screen -dmS minecraft ./run.sh`.
   - Путь: `/home/unclebug/storage/minecraft/`.
   - Управление:
     ```bash
     sudo systemctl status minecraft
     sudo systemctl restart minecraft
     # Доступ в консоль:
     screen -r minecraft
     # Отправка команды без входа в screen:
     screen -S minecraft -X stuff $'say Привет!\n'
     ```

2. **`minecraft-sync.path` + `minecraft-sync.service`**:
   - Следит за изменениями в `/home/unclebug/storage/minecraft/mods/`.
   - При добавлении/удалении `.jar` автоматически запускает:
     `python3 /home/unclebug/storage/minecraft/update_sync.py`
   - Копирует клиентские моды в `/home/unclebug/storage/zubrcraft-sync/mods/`, исключает серверные (`Chunky`, `spark`, `servercore`, `minimotd`) и генерирует свежий `manifest.json`.

3. **`playit.service`**:
   - Запускает демон `playit` для туннелирования портов через европейские Anycast ноды.

4. **`minecraft-playit-bridge.service`**:
   - Мост `socat TCP4-LISTEN:25566,fork,reuseaddr TCP4:127.0.0.1:25565` для стабильного коннекта Playit к Minecraft.

---

## 4. Папки и пути на сервере

- Сервер Minecraft: `/home/unclebug/storage/minecraft/`
  - Папка модов: `/home/unclebug/storage/minecraft/mods/`
  - Конфиги: `/home/unclebug/storage/minecraft/config/`
  - Мир: `/home/unclebug/storage/minecraft/world/`
  - Логи: `/home/unclebug/storage/minecraft/logs/latest.log`
- CDN хранилище синхронизации: `/home/unclebug/storage/zubrcraft-sync/`
  - Клиентские моды: `/home/unclebug/storage/zubrcraft-sync/mods/`
  - Манифест: `/home/unclebug/storage/zubrcraft-sync/manifest.json`
- Скрипт генератора манифеста: `/home/unclebug/storage/minecraft/update_sync.py`
