# HeadBlocks

[![Issue Github](https://img.shields.io/github/issues-raw/AerWyn81/HeadBlocks?color=%2370d121&style=for-the-badge)](https://github.com/AerWyn81/HeadBlocks/issues)
[![Discord](https://img.shields.io/discord/912462773995335701?label=DISCORD&logo=discord&logoColor=%238bc1f7&style=for-the-badge)](https://discord.gg/f3d848XsQt)
[![Spigot Download](https://img.shields.io/spiget/downloads/97630?logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAAAXNSR0IB2cksfwAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAGASURBVDhPYxg0gB2I2Xx8fDRERUV5jI2N%2BSHCCMAIpZEBk6%2Bv73x2dnYNQUFB1Y8fP15kZWVdsHTp0oVQeTBghtLI4H9kZOSmGzdunGNkZLQ%2Ff%2F68w8GDB89C5eCACUqjgMbGxn9AG6O4ubkN9PX1vaHCKADs1MIQC84%2F7P%2BEf3L%2FfQnii%2F5ilDjzXPgYUKMMLxfrET7Bjy6TJ%2B%2F4CZKDAZBTGf2MP603kP7cyvzjT5Qcy9tEWaHvifysX%2BSkuV4x64s8kHv2UYKBk1%2Fq9MOHD39BtEE0skiJC7k%2BfM9pzsn2X%2FzuW27JD1%2F%2Bi3Ky%2FGR%2B%2B42d4eEHPoZ773nsuLi4Ynh4eOY%2Ff%2F4cbDPYqW5ubipfvnyZxMzMJPLv3%2F8D379%2F%2FcXBxmHJzMoKioanv3%2F%2F1gY6WxkYug%2Ffvn1rfPr06bfw6Kivr2c6cOAAEzAE%2F0CFwHKurq5cQEMvMjExyf7792%2Ff%2F%2F%2F%2Fo0%2BcOPEOrIIQsLe3V7C2tpYDMrHGAj0AAwMAnm2Bn%2B%2FKtQMAAAAASUVORK5CYII%3D&style=for-the-badge)](https://www.spigotmc.org/resources/headblocks-updated-1-20-easter-eggs-multi-server-support-fully-translatable-free.97630/)
[![bStats Players](https://img.shields.io/bstats/players/15495?style=for-the-badge)](https://bstats.org/plugin/bukkit/HeadBlocks/15495)
[![bStats Servers](https://img.shields.io/bstats/servers/15495?style=for-the-badge)](https://bstats.org/plugin/bukkit/HeadBlocks/15495)
[![Translations](https://img.shields.io/badge/Translations-2E3340.svg?style=for-the-badge&logo=Crowdin&logoColor=white)](https://crowdin.com/project/headblocks)

___

### OVERVIEW:

Welcome to the **Headblocks** official code repository.

* If you want functional information about the plugin or download it, you can click on the following
  link: [Spigot](https://www.spigotmc.org/resources/headblocks-1-8-1-17.97630/)
* If you want technical information about the plugin, you are at the right place!

The plugin is compatible with servers running in **1.20 to 1.21+** and is compiled with **Java 17**.

### TRANSLATIONS:

Feel free to contribute or download community translations on [Crowdin](https://crowdin.com/project/headblocks).  
Thanks for contributing! ❤️

---

### LINKS:

* Spigot plugin page: https://www.spigotmc.org/resources/headblocks.97630/
* Discord: https://discord.gg/f3d848XsQt

___

### THIRD PARTY:

I'm using some third party libraries:

* [Item-NBT-API](https://github.com/tr7zw/Item-NBT-API): Used to handle heads textures
* [ConfigUpdater](https://github.com/tchristofferson/Config-Updater): Used to update the config.yml and message_XX.yml
  at each update
* XSound, XParticle from [XSeries](https://github.com/CryptoMorin/XSeries): Used to translate song name to the correct
  MC version and particles

I'm not using some bStats metrics or Auto-Updater.

___

### API:

#### You can get the HeadBlocks plugin instance:

```
package package.artifactId;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        HeadBlocks headBlocksInstance = (HeadBlocks) this.getServer().getPluginManager().getPlugin("HeadBlocks");

        if (headBlocksInstance == null) {
            // HeadBlocks not enabled
        }

        // Access at the entire plugin with the headBlocksInstance instance
        headBlocksInstance..
    }
```

#### And use:

| Events           | Explanations                                                       |
|------------------|--------------------------------------------------------------------|
| HeadClickEvent   | _Event triggered when a head is clicked (contains success or not)_ |
| HeadCreatedEvent | _Event called when a head is created (contains the location)_      |
| HeadDeletedEvent | _Event to trigger when a head is deleted_                          |

### WIKI:

Find the wiki **[here](https://aerwyn81.github.io/HeadBlocks)**
___
It's my first plugin, it's not perfect yet, don't hesitate to tell me bugs or suggestions on discord or in issue :)
Thank you for your interest ❤️