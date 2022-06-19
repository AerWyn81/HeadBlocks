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
  <version>LATEST_RELEASE</version>
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