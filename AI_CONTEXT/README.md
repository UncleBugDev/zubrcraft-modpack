# Полный контекст и проект ZubrCraft для Claude / других ИИ

Эта папка содержит исчерпывающие исходные файлы, конфигурации, архитектуру и рабочие инструменты для создания, сборки, деплоя и тестирования модов сервера Minecraft **ZubrCraft**.

---

## 📁 Структура каталога

```text
AI_CONTEXT/
├── README.md                      # Данный путеводитель
├── CLAUDE_PROMPT.md               # Главный системный промпт для Claude
├── SERVER_ARCHITECTURE.md         # Детальная архитектура сетей, портов и служб
├── MOD_DEVELOPMENT_TEMPLATE.md    # Шаблон и сниппеты для разработки мода Forge 1.20.1
│
├── CHATS/                         # Сюда складывать экспорты чатов с нейросетями (.txt / .md)
│   └── README.md
│
└── PROJECT_FILES/                 # ИСХОДНЫЕ ФАЙЛЫ И ИНСТРУМЕНТЫ ПРОЕКТА
    ├── zubrcraft_auth/            # Полный исходный код нашего кастомного мода авторизации
    │   ├── src/net/zubrcraft/auth/
    │   │   ├── ZubrCraftAuth.java (главный класс @Mod)
    │   │   ├── client/AuthScreen.java (кастомный GUI логина)
    │   │   ├── server/AuthManager.java (хэширование SHA-256)
    │   │   └── network/ (клиент-серверные пакеты)
    │   ├── resources/META-INF/mods.toml
    │   └── pack.mcmeta
    │
    ├── client/                    # Конфиги клиентской части
    │   ├── instance.cfg           # Конфигурация лаунчера Prism Launcher
    │   ├── sync.ps1               # Клиентский скрипт авто-синхронизации модов (PreLaunch)
    │   └── servers.dat            # Настроенный список серверов (Основной и Резервный)
    │
    ├── server/                    # Конфиги сервера
    │   ├── server.properties      # Настройки Forge-сервера
    │   ├── update_sync.py         # Скрипт индексации CDN манифеста
    │   ├── config/                # Конфигурации серверных модов
    │   │   ├── tab/               # Настройки мода TAB (config.yml, animations.yml, users.yml)
    │   │   └── minimotd/          # Настройки градиентного MOTD (main.conf)
    │   └── systemd/               # Службы Ubuntu
    │       ├── minecraft.service
    │       ├── minecraft-sync.path
    │       ├── minecraft-sync.service
    │       └── minecraft-playit-bridge.service
    │
    └── tools/                     # АВТОМАТИЧЕСКИЕ ИНСТРУМЕНТЫ ДЛЯ CLAUDE
        ├── check_server.py        # Диагностика: пинг 25565, WAN, резервный туннель, CDN
        ├── server_cmd.py          # Выполнение команд в консоли сервера и чтение логов
        └── deploy_mod.py          # Авто-деплой мода: загрузка .jar, перезапуск, проверка логов
```

---

## 🛠️ Инструменты автоматизации для Claude

Когда Claude подключен через Claude Code, терминал или рабочий каталог, он может самостоятельно выполнять следующие команды для тестирования:

1. **Проверить статус и доступность сервера:**
   ```bash
   python AI_CONTEXT/PROJECT_FILES/tools/check_server.py
   ```
   *Проверяет локальный сервер, домен `zubrcraft.fun`, Anycast туннель и CDN манифест.*

2. **Отправить команду в серверную консоль Minecraft:**
   ```bash
   python AI_CONTEXT/PROJECT_FILES/tools/server_cmd.py "list"
   python AI_CONTEXT/PROJECT_FILES/tools/server_cmd.py "say Привет из Claude!"
   python AI_CONTEXT/PROJECT_FILES/tools/server_cmd.py "tab reload"
   ```

3. **Собрать, задеплоить и протестировать новый мод в один клик:**
   ```bash
   python AI_CONTEXT/PROJECT_FILES/tools/deploy_mod.py "path/to/my_new_mod.jar"
   ```
   *Скрипт автоматически:*
   - Заливает `.jar` на сервер `unclebug@192.168.100.51:/home/unclebug/storage/minecraft/mods/`.
   - Перезапускает службу `minecraft.service`.
   - Слушает `latest.log` в реальном времени до готовности мира (`Done (X.Xs)`).
   - В случае ошибок сразу читает свежий краш-репорт и выводит причину падения!

---

## 🚀 Как работать с Claude

- **Для Claude Projects:** Загрузи папку `AI_CONTEXT` в Project Knowledge. В ней лежат как текстовые инструкции, так и реальные Java-исходники и конфиги.
- **Для Claude Code CLI:** Claude прямо в терминале видит репозиторий, может запускать тесты через скрипты из папки `tools/` и деплоить моды на сервер.
