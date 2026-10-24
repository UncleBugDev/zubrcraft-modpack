# ZubrCraft - Полный контекст проекта для Claude / AI

Добро пожаловать в проект **ZubrCraft**! Этот документ содержит исчерпывающее описание архитектуры, окружения, используемых технологий и инструкций по разработке кастомных Forge-модов для нашего сервера Minecraft.

---

## 1. Общие сведения о проекте

- **Игра:** Minecraft 1.20.1
- **Загрузчик модов:** Minecraft Forge (рекомендуемая версия `47.3.11`)
- **Версия Java:** Java 17 (OpenJDK 17)
- **Цель проекта:** Ламповый полуванильный сервер (SMP) с голосовым чатом, приятными декорациями, улучшенным строительством, физикой рубки деревьев и оптимизацией производительности.
- **Публичные домены:**
  - `zubrcraft.fun` (порт 25565) - основной прямой IP для входа (Белтелеком WAN `86.57.216.9`).
  - `vivo-whomever.tun.ply.gg:43020` - защищенный европейский Anycast-туннель Playit (резервный адрес для игроков с VPN Surfshark/NordVPN и для скрытия реального IP).
  - `sync.zubrcraft.fun` (порт 443, HTTPS через Cloudflare Tunnel) - скоростной CDN раздачи модов и манифеста синхронизации.
  - Сайт-инструкция для игроков: [https://unclebugdev.github.io/zubrcraft-modpack/](https://unclebugdev.github.io/zubrcraft-modpack/)
  - GitHub репозиторий: [UncleBugDev/zubrcraft-modpack](https://github.com/UncleBugDev/zubrcraft-modpack)

---

## 2. Инфраструктура и серверная архитектура

### Сервер (`unclebug-server`):
- **ОС:** Ubuntu 24.04 LTS (x86_64, Linux kernel 6.8).
- **Локальный адрес:** `192.168.100.51`
- **Tailscale адрес:** `100.85.40.18`
- **Расположение сервера Minecraft:** `/home/unclebug/storage/minecraft/`
- **Управление сервером:**
  - Служба systemd: `minecraft.service`
  - Запуск происходит внутри GNU Screen с именем сессии `minecraft`.
  - Отправка команд в консоль: `screen -S minecraft -X stuff $'КОМАНДА\n'`
  - Перезапуск сервера: `sudo systemctl restart minecraft.service`
  - Логи: `/home/unclebug/storage/minecraft/logs/latest.log`

### Система авто-синхронизации модов (Pre-Launch Sync):
Мы полностью отказались от мода AutoModpack из-за нестабильности сетевого протокола и лишних экранов подтверждений. Вместо него работает полностью бесшовная система:
1. **На стороне сервера:**
   - Путь раздачи CDN: `/home/unclebug/storage/zubrcraft-sync/` (Nginx порт 8085 -> Cloudflare Tunnel `sync.zubrcraft.fun`).
   - Папка с клиентскими модами: `/home/unclebug/storage/zubrcraft-sync/mods/`.
   - Файл манифеста: `/home/unclebug/storage/zubrcraft-sync/manifest.json`.
   - Фоновая служба `minecraft-sync.path` + `minecraft-sync.service`: автоматически следит за папкой `/home/unclebug/storage/minecraft/mods/`. Как только туда добавляется или удаляется любой `.jar`, служба мгновенно запускает `/home/unclebug/storage/minecraft/update_sync.py`, копирует мод на CDN и обновляет `manifest.json`.
2. **На стороне клиента (Prism Launcher / PineconeMC):**
   - В `instance.cfg` включена директива `PreLaunchCommand`.
   - Перед стартом Java запускается скрытый powershell-скрипт `sync.ps1`.
   - Он за 0.5-0.9 секунды обращается к `https://sync.zubrcraft.fun/manifest.json`.
   - Скачивает недостающие моды, удаляет устаревшие, обновляет список серверов (`servers.dat`).
   - Игра запускается с уже готовым и синхронизированным набором модов без единого вопроса игроку.

---

## 3. Список установленных модов (41 мод)

1. **Геймплей, механики и фермерство:**
   - `FarmersDelight-1.20.1-1.3.4.jar` - кулинария и фермерство.
   - `miners_delight-1.20.1-1.4.5-backport.jar` - расширение шахтерской еды.
   - `TreeChop-1.20.1-forge-0.19.0.jar` - реалистичная послойная рубка деревьев.
   - `comforts-forge-6.4.0+1.20.1.jar` - гамаки и спальные мешки (пропуск дня/ночи).
   - `waystones-forge-1.20.1-14.1.21.jar` - путеводные камни для телепортации.
   - `sophisticatedbackpacks-1.20.1-3.26.13.2215.jar` - улучшенные рюкзаки.
   - `sophisticatedcore-1.20.1-1.5.14.2419.jar` - библиотека рюкзаков.
   - `toms_storage-1.20-1.7.1.jar` - удобные хранилища и терминалы.
   - `naturalist-5.0pre4+forge-1.20.1.jar` - новые животные и фауна.

2. **Декор и строительство:**
   - `chipped-forge-1.20.1-3.0.7.jar` - сотни вариаций блоков на верстаках.
   - `handcrafted-forge-1.20.1-3.0.6.jar` - мебель, посуда и предметы интерьера.
   - `supplementaries-1.20-3.1.44-forge.jar` - интерактивные декоративные блоки.
   - `moonlight-1.20-2.16.37-forge.jar` - библиотека для Supplementaries.
   - `mcw-bridges-3.1.2-mc1.20.1forge.jar` - мосты.
   - `mcw-doors-1.1.5-mc1.20.1forge.jar` - новые двери и люки.
   - `athena-forge-1.20.1-3.1.2.jar` - библиотека подключенных текстур.
   - `resourcefullib-forge-1.20.1-2.1.29.jar` - служебная библиотека.

3. **Связь и безопасность:**
   - `voicechat-forge-1.20.1-2.6.24.jar` - позиционный голосовой чат Simple Voice Chat (порт 24454 UDP через Playit).
   - `open-parties-and-claims-forge-1.20.1-0.33.0.jar` - система приватов и пати.
   - `zubrcraft_auth-1.20.1-1.0.0.jar` - наш собственный кастомный мод авторизации с красивым GUI-экраном логина/пароля.

4. **Интерфейс (HUD) и клиентские моды:**
   - `jei-1.20.1-forge-15.74.0.229.jar` - просмотр рецептов.
   - `Jade-1.20.1-Forge-11.13.3.jar` - всплывающие подсказки с информацией о блоке.
   - `appleskin-forge-mc1.20.1-2.5.1.jar` - отображение сытости и насыщения.
   - `MouseTweaks-forge-mc1.20.1-2.25.1.jar` - удобное управление мышью в инвентаре.

5. **Оптимизация производительности:**
   - `embeddium-0.3.31+mc1.20.1.jar` - форк Sodium (замена OptiFine, дает 300+ FPS).
   - `oculus-mc1.20.1-1.8.0.jar` - поддержка шейдеров под Embeddium (форк Iris).
   - `modernfix-forge-5.27.89+mc1.20.1.jar` - ускорение запуска, фикс утечек структур.
   - `ferritecore-6.0.1-forge.jar` - экономия RAM на моделях блоков (-40% памяти).
   - `canary-mc1.20.1-0.3.3.jar` - оптимизация ИИ мобов и тиков мира (форк Lithium).
   - `saturn-mc1.20.1-0.1.3.jar` - оптимизация памяти и чанков.
   - `memoryleakfix-forge-1.17+-1.1.5.jar` - исправление утечек памяти ванильного клиента.
   - `noisium-forge-2.3.0+mc1.20-1.20.1.jar` - оптимизация генерации мира.
   - `smoothboot-reloaded-mc1.20.1-0.0.4.jar` - распределение нагрузки по ядрам CPU при старте.
   - `entityculling-forge-1.11.3-mc1.20.1.jar` - отсечение отрисовки невидимых мобов.
   - `packetfixer-3.3.2-1.18-1.20.4-merged.jar` - защита от крашей большими пакетами.
   - `alternate_current-mc1.20-1.7.0.jar` - быстрый редстоун без лагов.

6. **Серверные моды:**
   - `minimotd-reforged-mc1.19.4+1.20.x-2.0.13.jar` - красивый динамический градиентный MOTD со случайной ротацией.
   - `servercore-forge-1.5.2+1.20.1.jar` - серверный оптимизатор тиков и дальности прорисовки.
   - `Chunky-1.3.146.jar` - генератор чанков мира.
   - `spark-1.10.53-forge.jar` - серверный профайлер лагов.

---

## 4. Разработка новых кастомных модов для ZubrCraft

Если тебе поручено написать новый мод для нашего сервера:

### Основные требования:
1. **Версия:** Minecraft 1.20.1, Minecraft Forge 47.3.11+.
2. **Язык:** Java 17.
3. **MDK / Структура проекта:**
   - `META-INF/mods.toml` - обязательное описание мода (ID, имя, версия `1.20.1`, зависимости).
   - `pack.mcmeta` - pack format 15 (для 1.20.1).
   - Сепарация Client / Server:
     - Все клиентские визуальные элементы (экраны, рендереры, звуки, GUI) должны регистрироваться через `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` или `@OnlyIn(Dist.CLIENT)`.
     - Серверная логика должна безопасно работать на выделенном сервере без вызова клиентских классов Minecraft (`Minecraft.getInstance()`).
4. **Сетевой протокол (Network Packets):**
   - Используй `NetworkRegistry.newSimpleChannel` с уникальным `ResourceLocation("zubrcraft_<modname>", "main")`.
   - Версия протокола - строковый хэш или `"1"`.
   - Регистрация сообщений: `channel.registerMessage(index, MessageClass.class, MessageClass::encode, MessageClass::decode, MessageClass::handle)`.
5. **Пример нашего мода `zubrcraft_auth`:**
   В репозитории есть готовый пример кастомного мода в папке `zubrcraft_auth/`:
   - `src/net/zubrcraft/auth/ZubrCraftAuth.java` - главный класс мода и регистрация сетевых каналов.
   - `src/net/zubrcraft/auth/client/AuthScreen.java` - кастомный экран ввода пароля (GUI).
   - `src/net/zubrcraft/auth/server/AuthManager.java` - серверная логика хэширования паролей (SHA-256 с солью) и авторизации.
   - `src/net/zubrcraft/auth/network/` - классы пакетов клиент-серверного взаимодействия.

### Как мод деплоится на сервер:
1. Собирается файл `.jar` (например, `zubrcraft_custom-1.20.1-1.0.0.jar`).
2. Копируется на сервер в `/home/unclebug/storage/minecraft/mods/`.
3. Служба `minecraft-sync.path` на сервере автоматически подхватывает файл, копирует его в CDN `sync.zubrcraft.fun/mods/` и обновляет `manifest.json`.
4. Сервер перезапускается: `sudo systemctl restart minecraft.service`.
5. Игроки при следующем нажатии «Запуск» в лаунчере автоматически получают этот мод через `sync.ps1`.

---

## 5. Полезные команды и переменные

- **SSH доступ к серверу:** `ssh unclebug@192.168.100.51` (или через Tailscale `100.85.40.18`).
- **Папка модов:** `/home/unclebug/storage/minecraft/mods/`
- **Консоль сервера:** `screen -r minecraft` (для выхода нажать `Ctrl + A`, затем `D`).
- **Синхронизатор модов вручную (если нужно):** `python3 /home/unclebug/storage/minecraft/update_sync.py`
- **Проверить манифест модов:** `curl -s https://sync.zubrcraft.fun/manifest.json`
