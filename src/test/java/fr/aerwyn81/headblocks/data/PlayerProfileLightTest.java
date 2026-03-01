package fr.aerwyn81.headblocks.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerProfileLightTest {

    @Test
    void fullConstructor_setsAllFields() {
        UUID uuid = UUID.randomUUID();
        PlayerProfileLight profile = new PlayerProfileLight(uuid, "Steve", "&6Steve");

        assertThat(profile.uuid()).isEqualTo(uuid);
        assertThat(profile.name()).isEqualTo("Steve");
        assertThat(profile.customDisplay()).isEqualTo("&6Steve");
    }

    @Test
    void uuidOnlyConstructor_setsEmptyNameAndCustomDisplay() {
        UUID uuid = UUID.randomUUID();
        PlayerProfileLight profile = new PlayerProfileLight(uuid);

        assertThat(profile.uuid()).isEqualTo(uuid);
        assertThat(profile.name()).isEmpty();
        assertThat(profile.customDisplay()).isEmpty();
    }

    @Test
    void uuid_returnsCorrectValue() {
        UUID uuid = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        PlayerProfileLight profile = new PlayerProfileLight(uuid, "Alex", "");

        assertThat(profile.uuid()).isEqualTo(uuid);
    }
}
