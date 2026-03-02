package fr.aerwyn81.headblocks.databases;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumTypeDatabaseTest {

    @Test
    void of_MySQL_returnsMySQL() {
        assertThat(EnumTypeDatabase.of("MySQL")).isEqualTo(EnumTypeDatabase.MySQL);
    }

    @Test
    void of_SQLite_returnsSQLite() {
        assertThat(EnumTypeDatabase.of("SQLite")).isEqualTo(EnumTypeDatabase.SQLite);
    }

    @Test
    void of_invalid_returnsNull() {
        assertThat(EnumTypeDatabase.of("Postgres")).isNull();
    }

    @Test
    void of_null_returnsNull() {
        assertThat(EnumTypeDatabase.of(null)).isNull();
    }
}
