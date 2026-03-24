# Storage

By default, HeadBlocks stores all data locally using SQLite (`plugins/HeadBlocks/headblocks.db`). For multi-server setups, you can use MySQL/MariaDB and Redis.

## MySQL / MariaDB

```yaml
database:
  enable: false
  type: MySQL
  settings:
    hostname: localhost
    database: ''
    username: ''
    password: ''
    port: 3306
    ssl: false
    prefix: ''
    pool:
      maxConnections: 10
      minIdleConnections: 2
      connectionTimeout: 5
      idleTimeout: 300
      maxLifetime: 1800
```

| Option       | Description                                              |
|--------------|----------------------------------------------------------|
| **type**     | `MySQL` or `MariaDB`                                     |
| **hostname** | `localhost` or URL to the remote database                |
| **database** | Database name                                            |
| **username** | User with read/write access                              |
| **password** | Password for connection                                  |
| **port**     | Connection port (default: `3306`)                        |
| **ssl**      | Enable SSL requests                                      |
| **prefix**   | Table prefix (e.g., `srv1_`). Use underscore to separate |

### Connection Pool

Advanced settings — most users should not need to change these:

| Option                 | Default | Description                              |
|------------------------|---------|------------------------------------------|
| **maxConnections**     | 10      | Maximum connections in the pool          |
| **minIdleConnections** | 2       | Minimum idle connections maintained      |
| **connectionTimeout**  | 5s      | Max time to wait for a connection        |
| **idleTimeout**        | 300s    | Time before an idle connection is closed |
| **maxLifetime**        | 1800s   | Maximum lifetime of a connection         |

{% hint style="warning" %}
Switching from SQLite to MySQL does **not** migrate data automatically. Use `/hb export` to export your data first.
{% endhint %}

## Redis (Multi-Server)

```yaml
redis:
  enable: false
  settings:
    hostname: localhost
    database: 0
    password: ''
    port: 6379
```

Redis enables real-time synchronization across multiple servers:

| Option       | Description                       |
|--------------|-----------------------------------|
| **hostname** | `localhost` or URL to Redis       |
| **database** | Redis database number (0-15)      |
| **password** | Connection password               |
| **port**     | Connection port (default: `6379`) |

{% hint style="warning" %}
Redis requires a MySQL/MariaDB database connection to work. SQLite alone is not sufficient for multi-server setups.
{% endhint %}
