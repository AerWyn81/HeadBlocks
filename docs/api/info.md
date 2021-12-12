# API:

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
  <version><LATEST RELEASE></version>
</dependency>
```

#### Using Gradle:

```
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.AerWyn81:HeadBlocks:<LATEST RELEASE>'
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
HeadClickEvent | _Event triggered when a head is clicked (contains success or not)_
HeadCreatedEvent | _Event called when a head is created (contains the location)_
HeadDeletedEvent | _Event to trigger when a head is deleted_

Methods  | Explanations
------------- | -------------
`getPlayerHeads(UUID playerUuid)` | _Return a `List<UUID>` of all related head found by the player_
`getTotalHeadSpawnCount()` | _Return an `int` which corresponds to the total of heads placed_
`getLeftPlayerHeadToMax(UUID playerUuid)` | _Return an `int` showing the amount of remaining heads to be found_