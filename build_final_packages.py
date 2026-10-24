import zipfile, os

MODS_DIR = 'build_optimization/mods'
SERVERS_DAT = 'servers.dat'
OPTIONS_TXT = 'options.txt'
ICON_FILE = 'mod_icon.png'

# 1. Build 1_ZubrCraft_Optimization_Base.zip
print("Building 1_ZubrCraft_Optimization_Base.zip...")
with zipfile.ZipFile('1_ZubrCraft_Optimization_Base.zip', 'w', compression=zipfile.ZIP_DEFLATED) as z:
    z.write(SERVERS_DAT, 'servers.dat')
    z.write(OPTIONS_TXT, 'options.txt')
    for f in sorted(os.listdir(MODS_DIR)):
        if f.endswith('.jar'):
            path = os.path.join(MODS_DIR, f)
            z.write(path, f'mods/{f}')
            print(f"  Added mod: {f}")

print("1_ZubrCraft_Optimization_Base.zip created! Size:", os.path.getsize('1_ZubrCraft_Optimization_Base.zip'))

# Read instance.cfg and mmc-pack.json
instance_cfg = """InstanceType=OneSix
name=ZubrCraft
iconKey=icon
OverrideCommands=false
OverrideJavaArgs=false
OverrideJavaLocation=false
OverrideMemory=true
MinMemAlloc=2048
MaxMemAlloc=4096
AutoCloseConsole=true
notes=Официальная сборка сервера ZubrCraft (zubrcraft.fun)
"""

mmc_pack = """{
    "components": [
        {
            "cachedName": "Minecraft",
            "cachedVersion": "1.20.1",
            "important": true,
            "uid": "net.minecraft",
            "version": "1.20.1"
        },
        {
            "cachedName": "Forge",
            "cachedVersion": "47.3.11",
            "important": true,
            "uid": "net.minecraftforge",
            "version": "47.3.11"
        }
    ],
    "formatVersion": 1
}
"""

# 2. Build ZubrCraft-Prism.zip
print("Building ZubrCraft-Prism.zip...")
with zipfile.ZipFile('ZubrCraft-Prism.zip', 'w', compression=zipfile.ZIP_DEFLATED) as z:
    z.writestr('instance.cfg', instance_cfg)
    z.writestr('mmc-pack.json', mmc_pack)
    z.write(ICON_FILE, 'icon.png')
    z.write(SERVERS_DAT, '.minecraft/servers.dat')
    z.write(OPTIONS_TXT, '.minecraft/options.txt')
    for f in sorted(os.listdir(MODS_DIR)):
        if f.endswith('.jar'):
            path = os.path.join(MODS_DIR, f)
            z.write(path, f'.minecraft/mods/{f}')

print("ZubrCraft-Prism.zip created! Size:", os.path.getsize('ZubrCraft-Prism.zip'))
