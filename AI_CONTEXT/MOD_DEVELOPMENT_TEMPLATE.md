# Шаблон разработки мода под Forge 1.20.1 для Claude

Этот документ содержит готовые сниппеты и рекомендации для Claude при создании новых модов для сервера ZubrCraft.

---

## 1. Стек и версии

- **Minecraft:** `1.20.1`
- **Forge:** `47.3.11` (или совместимая 47.3.x)
- **Java:** `17`
- **Mapping:** `official 1.20.1`

---

## 2. Структура проекта мода

```text
my_mod/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/
    └── main/
        ├── java/
        │   └── net/zubrcraft/mymod/
        │       ├── MyMod.java                     (главный класс @Mod)
        │       ├── client/                        (ТОЛЬКО клиентский код: экраны, рендеры)
        │       │   └── MyModClient.java
        │       ├── server/                        (серверная логика, базы данных, обработка)
        │       │   └── MyModServer.java
        │       └── network/                       (сетевые пакеты между клиентом и сервером)
        │           ├── MyModNetwork.java
        │           └── packet/
        │               └── ExamplePacket.java
        └── resources/
            ├── META-INF/
            │   └── mods.toml                      (метаданные мода)
            └── pack.mcmeta                        (pack_format 15)
```

---

## 3. Пример `mods.toml` (`src/main/resources/META-INF/mods.toml`)

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"

[[mods]]
modId="zubrcraft_mymod"
version="1.0.0"
displayName="ZubrCraft MyMod"
authors="UncleBug"
description='''
Кастомный мод для сервера ZubrCraft.
'''

[[dependencies.zubrcraft_mymod]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.zubrcraft_mymod]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
```

---

## 4. Пример `pack.mcmeta` (`src/main/resources/pack.mcmeta`)

```json
{
  "pack": {
    "description": "ZubrCraft Resources",
    "pack_format": 15
  }
}
```

---

## 5. Главный класс мода (`MyMod.java`)

```java
package net.zubrcraft.mymod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "zubrcraft_mymod";
    public static final Logger LOGGER = LogManager.getLogger();

    public MyMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Регистрация на шине событий Forge
        MinecraftForge.EVENT_BUS.register(this);

        // Безопасная инициализация клиентской части
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.zubrcraft.mymod.client.MyModClient.init(modEventBus);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Регистрация сетевых пакетов
            net.zubrcraft.mymod.network.MyModNetwork.register();
        });
    }
}
```

---

## 6. Сетевой канал (Network Packets) под Forge 1.20.1

```java
package net.zubrcraft.mymod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.zubrcraft.mymod.MyMod;

public class MyModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(MyMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // CHANNEL.registerMessage(packetId++, PacketClass.class, PacketClass::encode, PacketClass::decode, PacketClass::handle);
    }
}
```

---

## 7. Жизненный цикл деплоя на сервер ZubrCraft

1. Скомпилировать `.jar`: `./gradlew build`
2. Положить `.jar` в `/home/unclebug/storage/minecraft/mods/`
3. Автоматический демон `minecraft-sync.path` сразу:
   - Обновит `sync.zubrcraft.fun/manifest.json`.
   - Зальет мод в CDN `/home/unclebug/storage/zubrcraft-sync/mods/`.
4. Перезапустить сервер: `sudo systemctl restart minecraft`.
5. Игроки запустят лаунчер и сразу получат мод в автоматическом режиме.
