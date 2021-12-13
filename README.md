# HeadBlocks

![Issue Github](https://img.shields.io/github/issues-raw/AerWyn81/HeadBlocks?color=%2370d121&style=for-the-badge)
![Discord](https://img.shields.io/discord/912462773995335701?label=DISCORD&logo=discord&logoColor=%238bc1f7&style=for-the-badge)
![Code Quality](https://img.shields.io/codefactor/grade/github/AerWyn81/HeadBlocks?logo=codefactor&style=for-the-badge)
___

### OVERVIEW:

Welcome to the **Headblocks** official code repository.

* If you want functional information about the plugin or download it, you can click on the following
  link: [Spigot](https://www.spigotmc.org/resources/headblocks-1-8-1-17.97630/)
* If you want technical information about the plugin, you are at the right place!

The plugin is compatible with servers running in **1.8 to 1.18+** and is compiled with **Java 8**.

I'm using some third party libraries:

* [Item-NBT-API](https://github.com/tr7zw/Item-NBT-API): Used to handle heads textures
* [ConfigUpdater](https://github.com/tchristofferson/Config-Updater): Used to update the config.yml and message_XX.yml
  at each update
* XSound, XParticle from [XSeries](https://github.com/CryptoMorin/XSeries): Used to translate song name to the correct
  MC version and particles

I'm not using some bStats metrics or Auto-Updater.

Links:

* Spigot plugin page: https://www.spigotmc.org/resources/headblocks.97630/
* Discord: https://discord.gg/f3d848XsQt

___

### API:

#### Using Maven:

```
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.AerWyn81</groupId>
  <artifactId>HeadBlocks</artifactId>
  <version>1.3.11</version>
</dependency>
```

#### Using Gradle:

```
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.AerWyn81:HeadBlocks:1.3.11'
}
```

#### Now you can get the HeadBlocks plugin instance:

```
package package.artifactId;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        HeadBlocks headBlocksInstance = (HeadBlocks) this.getServer().getPluginManager().getPlugin("HeadBlocks");

        if (headBlocksInstance == null) {
            // HeadBlocks not enabled
        }

        // Get HeadBlockAPI instance for methods
        HeadBlocksAPI headBlocksAPI = headBlocksInstance.getHeadBlocksAPI();
        
        // Access at the entire plugin with the headBlocksInstance instance
        headBlocksInstance..
    }
```

#### And use:

Events  | Explanations
------------- | -------------
HeadClickEvent | _Event triggered when a head is clicked_
HeadCreatedEvent | _Event called when a head is created_
HeadDeletedEvent | _Event to trigger when a head is deleted_

Methods  | Explanations
------------- | -------------
`getPlayerHeads(UUID playerUuid)` | _Return a `List<UUID>` of all related head found by the player_
`getTotalHeadSpawnCount()` | _Return an `int` which corresponds to the total of heads placed_
`getLeftPlayerHeadToMax(UUID playerUuid)` | _Return an `int` showing the amount of remaining heads to be found_
___

### WIKI:

Find the wiki **[here](https://aerwyn81.github.io/HeadBlocks)**
___
It's my first plugin, it's not perfect yet, don't hesitate to tell me bugs or suggestions on discord or in issue :)
Thank you for your interest ❤️