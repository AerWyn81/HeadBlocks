# API

#### Using Maven

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

#### Using Gradle

```
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.AerWyn81:HeadBlocks:<LATEST RELEASE>'
}
```

#### Getting the HeadBlocks Plugin Instance

```
package package.artifactId;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        HeadBlocks headBlocksInstance = (HeadBlocks) this.getServer().getPluginManager().getPlugin("HeadBlocks");

        if (headBlocksInstance == null) {
            // HeadBlocks not enabled
        }

        // Access the plugin using the headBlocksInstance
        headBlocksInstance..
    }
```

#### Available Events

| Events               | Cancellable | Description                                                                                |
|----------------------|:-----------:|--------------------------------------------------------------------------------------------|
| HeadClickEvent       |     No      | _Triggered when a head is clicked (includes success status)_                               |
| HeadCreatedEvent     |     No      | _Called when a head is created (includes the location)_                                    |
| HeadDeletedEvent     |     No      | _Triggered when a head is deleted_                                                         |
| HuntCreateEvent      |     Yes     | _Fired before a new hunt is persisted. Cancel to prevent creation_                         |
| HuntDeleteEvent      |     Yes     | _Fired before a hunt is deleted. Cancel to prevent deletion_                               |
| HuntStateChangeEvent |     Yes     | _Fired before a hunt state changes (e.g., ACTIVE to INACTIVE). Provides old and new state_ |