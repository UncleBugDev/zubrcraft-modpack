import zipfile, io, json, os

SOURCE_JAR = 'build_optimization/mods/automodpack-mc1.20.1-forge-4.0.7.jar'
OUTPUT_JAR = 'build_optimization/mods/automodpack-mc1.20.1-forge-4.0.7.jar'

with open('mod_icon.png', 'rb') as f:
    ICON_BYTES = f.read()

# Load patched classes
with open('DataS2CPacket_patched.class', 'rb') as f:
    S2C_BYTES = f.read()

with open('DataC2SPacket_patched.class', 'rb') as f:
    C2S_BYTES = f.read()

# Patch ErrorScreen prefix
with open('ErrorScreen.class', 'rb') as f:
    error_screen = bytearray(f.read())

old_prefix = b'[AutoModpack] Error! ' # 21 bytes
new_prefix = b'[ZubrCraft Sync]     ' # 21 bytes (padded with spaces to preserve exact pool length)
idx = error_screen.find(old_prefix)
if idx != -1:
    error_screen[idx : idx + 21] = new_prefix
    print("Patched ErrorScreen.class prefix successfully!")
else:
    print("Warning: old_prefix not found directly in ErrorScreen, checking without space...")
    old_p2 = b'[AutoModpack] Error!'
    idx2 = error_screen.find(old_p2)
    if idx2 != -1:
        error_screen[idx2 : idx2 + 19] = b'[ZubrCraft] Error! '
        print("Patched ErrorScreen.class with 19-byte prefix!")

ERROR_SCREEN_BYTES = bytes(error_screen)

# Read ru_ru and en_us dictionaries
ru_dict = {
  "automodpack.restart.full": "Сборка ZubrCraft успешно загружена!",
  "automodpack.restart.update": "Обновление сборки ZubrCraft загружено!",
  "automodpack.restart.select": "Сборка сервера ZubrCraft выбрана!",
  "automodpack.restart.automodpack": "Модуль синхронизации ZubrCraft Sync обновлен!",
  "automodpack.restart.description": "Для применения всех файлов требуется перезапустить клиент Minecraft.",
  "automodpack.restart.secDescription": "Перезапустить игру прямо сейчас?",
  "automodpack.restart.confirm": "Да, перезапустить",
  "automodpack.restart.cancel": "Позже (вернуться)",

  "automodpack.danger": "СИНХРОНИЗАЦИЯ СБОРКИ ZUBRCRAFT",
  "automodpack.danger.description": "Сервер ZubrCraft подготовил для вас официальный набор модов и настроек.",
  "automodpack.danger.secDescription": "Все компоненты сборки проверены и синхронизированы с сервером.",
  "automodpack.danger.thiDescription": "Сборка включает оптимизацию, шейдеры и фирменные текстуры ZubrCraft.",
  "automodpack.danger.warning1": "Добро пожаловать на официальный сервер ZubrCraft!",
  "automodpack.danger.warning2": "Файлы будут автоматически загружены в папку вашей игры.",
  "automodpack.danger.warning3": "Подтвердите загрузку, чтобы продолжить подключение.",
  "automodpack.danger.warning4": "Официальный сайт проекта:",
  "automodpack.danger.warning5": "https://zubr-craft.vercel.app/",
  "automodpack.danger.warning6": "Все файлы абсолютно безопасны и проверены администрацией.",
  "automodpack.danger.warning7": "Нажмите кнопку ниже для старта синхронизации.",
  "automodpack.danger.confirm": "Начать синхронизацию",
  "automodpack.danger.cancel": "Отмена",

  "automodpack.danger.unverified": "ОБНОВЛЕНИЕ ФАЙЛОВ ZUBRCRAFT",
  "automodpack.danger.unverified.description": "Сервер обновил набор модов и конфигураций для комфортной игры.",
  "automodpack.danger.unverified.secDescription": "Новые файлы подготовлены командой ZubrCraft Team.",
  "automodpack.danger.unverified.thiDescription": "Они необходимы для правильной работы контента на сервере.",
  "automodpack.danger.unverified.warning1": "Все добавленные модификации проверены на совместимость и стабильность.",
  "automodpack.danger.unverified.warning2": "Синхронизация происходит в один клик.",
  "automodpack.danger.unverified.warning3": "Нажмите «Применить обновление» для быстрой загрузки.",
  "automodpack.danger.unverified.warning4": "При возникновении вопросов посетите наш сайт или дискорд.",
  "automodpack.danger.unverified.warning5": "Сервер: zubrcraft.fun",
  "automodpack.danger.unverified.warning6": "Сборка оптимизирована для высокого FPS и плавной игры.",
  "automodpack.danger.unverified.warning7": "Приятной игры на ZubrCraft!",
  "automodpack.danger.unverified.files": "Обновляемые файлы (%s):",
  "automodpack.danger.unverified.more": "и еще %s",
  "automodpack.danger.unverified.confirm": "Применить обновление",
  "automodpack.danger.acknowledge": "Я доверяю серверу ZubrCraft и подтверждаю загрузку.",

  "automodpack.changelog.view": "Список изменений сборки",
  "automodpack.changelog.noChanges": "Изменений в файлах не найдено.",
  "automodpack.changelog.openPage": "Открыть сайт ZubrCraft",

  "automodpack.download.calculating": "Анализ файлов сборки...",
  "automodpack.download.eta": "Осталось времени: %s",
  "automodpack.download.downloading": "Загрузка:",
  "automodpack.download.noFiles": "Все файлы актуальны! Загрузка не требуется.",

  "automodpack.redownload": "Переустановить сборку ZubrCraft",
  "automodpack.redownload.description": "Повторная загрузка и проверка целостности всех файлов сборки.",

  "automodpack.error": "Ошибка синхронизации",
  "automodpack.error.files": "Не удалось загрузить отдельные файлы сборки (проверьте интернет или состояние сервера).",
  "automodpack.error.critical": "Критическая ошибка: сервер синхронизации временно недоступен.",
  "automodpack.error.logs": "Технические детали записаны в logs/latest.log.",

  "automodpack.fetch": "Поиск прямых зеркал на официальных репозиториях...",
  "automodpack.fetch.found": "Найдено прямых ссылок: %s.",

  "automodpack.validation.title": "Проверка безопасности соединения",
  "automodpack.validation.description1": "Шифрованное соединение с официальным сервером ZubrCraft.",
  "automodpack.validation.description2": "Защищает от подмены файлов и обеспечивает целостность сборки.",
  "automodpack.validation.fingerprint.label": "Отпечаток сервера:",
  "automodpack.validation.confirm.text": "Отпечаток безопасности:",
  "automodpack.validation.verify": "Подтвердить",
  "automodpack.validation.skip.title": "Подтверждение доверия",
  "automodpack.validation.skip.warning1": "Подключение к серверу ZubrCraft.",
  "automodpack.validation.skip.warning2": "Убедитесь, что вы подключаетесь к официальному серверу zubrcraft.fun.",
  "automodpack.validation.skip.instruction": "Подтвердите доверие серверу, чтобы продолжить.",
  "automodpack.validation.skip.confirm.text": "Введите текст ниже для подтверждения:",
  "automodpack.validation.skip.failed": "Неверный текст подтверждения",

  "automodpack.retry": "Повторить попытку",
  "automodpack.select": "Выбрать сборку ZubrCraft",
  "automodpack.yes": "Да",
  "automodpack.no": "Нет",
  "automodpack.cancel": "Отмена",
  "automodpack.back": "Назад",
  "automodpack.wait": "Синхронизация с ZubrCraft, пожалуйста, подождите...",
  "automodpack.skip": "Пропустить",
  "automodpack.learnmore": "Сайт проекта"
}

en_dict = {
  "automodpack.restart.full": "ZubrCraft modpack installed successfully!",
  "automodpack.restart.update": "ZubrCraft modpack update downloaded!",
  "automodpack.restart.select": "ZubrCraft modpack selected!",
  "automodpack.restart.automodpack": "ZubrCraft Sync updated!",
  "automodpack.restart.description": "Please restart Minecraft to apply all files.",
  "automodpack.restart.secDescription": "Restart Minecraft now?",
  "automodpack.restart.confirm": "Yes, restart now",
  "automodpack.restart.cancel": "Later",
  "automodpack.danger": "ZUBRCRAFT MODPACK SYNC",
  "automodpack.danger.description": "The ZubrCraft server prepared official mods and configs for your game.",
  "automodpack.danger.secDescription": "All modpack components are verified and synchronized.",
  "automodpack.danger.thiDescription": "Includes performance optimizations, shaders, and ZubrCraft textures.",
  "automodpack.danger.warning1": "Welcome to the official ZubrCraft server!",
  "automodpack.danger.warning2": "Files will be automatically placed into your Minecraft folder.",
  "automodpack.danger.warning3": "Confirm download to continue connecting.",
  "automodpack.danger.warning4": "Official website:",
  "automodpack.danger.warning5": "https://zubr-craft.vercel.app/",
  "automodpack.danger.warning6": "All files are safe and verified by the server team.",
  "automodpack.danger.warning7": "Click the button below to start synchronization.",
  "automodpack.danger.confirm": "Start Sync",
  "automodpack.danger.cancel": "Cancel",
  "automodpack.danger.unverified": "ZUBRCRAFT FILE UPDATE",
  "automodpack.danger.unverified.description": "New mods and configurations are available for ZubrCraft.",
  "automodpack.danger.unverified.secDescription": "Files prepared by the ZubrCraft Team for optimal gameplay.",
  "automodpack.danger.unverified.thiDescription": "Required for server compatibility and features.",
  "automodpack.danger.unverified.warning1": "All files have been verified for stability.",
  "automodpack.danger.unverified.warning2": "1-click automatic update.",
  "automodpack.danger.unverified.warning3": "Click Apply Update to download.",
  "automodpack.danger.unverified.warning4": "Visit our website if you need assistance.",
  "automodpack.danger.unverified.warning5": "Server: zubrcraft.fun",
  "automodpack.danger.unverified.warning6": "Optimized for high FPS and smooth gameplay.",
  "automodpack.danger.unverified.warning7": "Have fun on ZubrCraft!",
  "automodpack.danger.unverified.files": "Updated files (%s):",
  "automodpack.danger.unverified.more": "and %s more",
  "automodpack.danger.unverified.confirm": "Apply Update",
  "automodpack.danger.acknowledge": "I trust ZubrCraft and confirm download.",
  "automodpack.changelog.view": "View Changelog",
  "automodpack.changelog.noChanges": "No changes found.",
  "automodpack.changelog.openPage": "Open ZubrCraft Website",
  "automodpack.download.calculating": "Analyzing modpack files...",
  "automodpack.download.eta": "Time remaining: %s",
  "automodpack.download.downloading": "Downloading:",
  "automodpack.download.noFiles": "All files are up to date!",
  "automodpack.redownload": "Reinstall ZubrCraft Modpack",
  "automodpack.redownload.description": "Re-download and verify all modpack files.",
  "automodpack.error": "Sync Error",
  "automodpack.error.files": "Failed to download some modpack files (check internet connection or server status).",
  "automodpack.error.critical": "Critical error: sync host server is temporarily unreachable.",
  "automodpack.error.logs": "Technical details have been written to logs/latest.log.",
  "automodpack.fetch": "Searching for official mirrors...",
  "automodpack.fetch.found": "Found %s direct URLs.",
  "automodpack.validation.title": "Secure Connection",
  "automodpack.validation.description1": "Encrypted connection to the official ZubrCraft server.",
  "automodpack.validation.description2": "Ensures file integrity and authentic updates.",
  "automodpack.validation.fingerprint.label": "Server Fingerprint:",
  "automodpack.validation.confirm.text": "Security Fingerprint:",
  "automodpack.validation.verify": "Confirm",
  "automodpack.validation.skip.title": "Trust Confirmation",
  "automodpack.validation.skip.warning1": "Connecting to ZubrCraft.",
  "automodpack.validation.skip.warning2": "Make sure you are connecting to zubrcraft.fun.",
  "automodpack.validation.skip.instruction": "Confirm trust to continue.",
  "automodpack.validation.skip.confirm.text": "Type the text below to confirm:",
  "automodpack.validation.skip.failed": "Invalid confirmation text",
  "automodpack.retry": "Try Again",
  "automodpack.select": "Select ZubrCraft Modpack",
  "automodpack.yes": "Yes",
  "automodpack.no": "No",
  "automodpack.cancel": "Cancel",
  "automodpack.back": "Back",
  "automodpack.wait": "Syncing with ZubrCraft, please wait...",
  "automodpack.skip": "Skip",
  "automodpack.learnmore": "Website"
}

ru_bytes = json.dumps(ru_dict, ensure_ascii=False, indent=2).encode('utf-8')
en_bytes = json.dumps(en_dict, ensure_ascii=False, indent=2).encode('utf-8')

# 1. Process inner jar
inner_output = io.BytesIO()
with zipfile.ZipFile(SOURCE_JAR, 'r') as outer:
    inner_raw = outer.read('META-INF/jarjar/automodpack-mod.jar')
    with zipfile.ZipFile(io.BytesIO(inner_raw), 'r') as inner_in:
        with zipfile.ZipFile(inner_output, 'w', compression=zipfile.ZIP_DEFLATED) as inner_out:
            for item in inner_in.infolist():
                name = item.filename
                if name == 'assets/automodpack/icon.png':
                    inner_out.writestr(item, ICON_BYTES)
                elif name == 'assets/automodpack/lang/ru_ru.json':
                    inner_out.writestr(item, ru_bytes)
                elif name == 'assets/automodpack/lang/en_us.json':
                    inner_out.writestr(item, en_bytes)
                elif name == 'META-INF/mods.toml' or name == 'META-INF/neoforge.mods.toml':
                    content = inner_in.read(name).decode('utf-8')
                    content = content.replace('displayName = "AutoModpack"', 'displayName = "ZubrCraft Sync"')
                    content = content.replace('authors = "Skidam"', 'authors = "UncleBug, ZubrCraft Team"')
                    content = content.replace('https://modrinth.com/mod/automodpack', 'https://zubr-craft.vercel.app/')
                    content = content.replace('description = "Enjoy a seamless modpack installation process and effortless updates with a user-friendly solution that simplifies management, making your gaming experience a breeze."', 'description = "Официальная система синхронизации сборки и ресурсов сервера ZubrCraft. Быстрая загрузка и проверка файлов перед входом."')
                    inner_out.writestr(item, content.encode('utf-8'))
                elif name == 'pl/skidam/automodpack/networking/packet/DataS2CPacket.class':
                    inner_out.writestr(item, S2C_BYTES)
                elif name == 'pl/skidam/automodpack/networking/packet/DataC2SPacket.class':
                    inner_out.writestr(item, C2S_BYTES)
                elif name == 'pl/skidam/automodpack/client/ui/ErrorScreen.class':
                    inner_out.writestr(item, ERROR_SCREEN_BYTES)
                else:
                    inner_out.writestr(item, inner_in.read(name))

NEW_INNER_JAR_BYTES = inner_output.getvalue()
print("Rebuilt inner jar, size:", len(NEW_INNER_JAR_BYTES))

# 2. Process outer jar
outer_output = io.BytesIO()
with zipfile.ZipFile(SOURCE_JAR, 'r') as outer_in:
    with zipfile.ZipFile(outer_output, 'w', compression=zipfile.ZIP_DEFLATED) as outer_out:
        for item in outer_in.infolist():
            name = item.filename
            if name == 'META-INF/jarjar/automodpack-mod.jar':
                outer_out.writestr(item, NEW_INNER_JAR_BYTES)
            elif name == 'icon.png':
                outer_out.writestr(item, ICON_BYTES)
            elif name == 'META-INF/mods.toml':
                content = outer_in.read(name).decode('utf-8')
                content = content.replace('displayName = "AutoModpack"', 'displayName = "ZubrCraft Sync"')
                content = content.replace('authors = "Skidam"', 'authors = "UncleBug, ZubrCraft Team"')
                content = content.replace('https://modrinth.com/mod/automodpack', 'https://zubr-craft.vercel.app/')
                content = content.replace("description = '''\n\n'''", "description = '''\nОфициальная система синхронизации и обновления сборки сервера ZubrCraft.\n'''")
                outer_out.writestr(item, content.encode('utf-8'))
            else:
                outer_out.writestr(item, outer_in.read(name))

with open('automodpack-custom.jar', 'wb') as f:
    f.write(outer_output.getvalue())

# Replace the one in build_optimization/mods
with open(OUTPUT_JAR, 'wb') as f:
    f.write(outer_output.getvalue())

print("Successfully created custom-branded AutoModpack JAR: automodpack-custom.jar!")
