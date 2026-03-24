# API

## Maven

```xml
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

## Gradle

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.github.AerWyn81:HeadBlocks:<LATEST RELEASE>'
}
```

## Getting the Plugin Instance

```java
package package.artifactId;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        HeadBlocks headBlocksInstance = (HeadBlocks) this.getServer()
            .getPluginManager().getPlugin("HeadBlocks");

        if (headBlocksInstance == null) {
            // HeadBlocks not enabled
            return;
        }

        // Access the plugin using the headBlocksInstance
    }
}
```

## Available Events

| Event | Cancellable | Description |
|---|:---:|---|
| **HeadClickEvent** | No | Triggered when a head is clicked (includes success status) |
| **HeadCreatedEvent** | No | Called when a head is created (includes the location) |
| **HeadDeletedEvent** | No | Triggered when a head is deleted |
| **HuntCreateEvent** | Yes | Fired before a new hunt is persisted. Cancel to prevent creation |
| **HuntDeleteEvent** | Yes | Fired before a hunt is deleted. Cancel to prevent deletion |
| **HuntStateChangeEvent** | Yes | Fired before a hunt state changes (e.g., ACTIVE → INACTIVE). Provides old and new state |
