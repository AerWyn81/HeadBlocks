package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class VersionUtilsTest {

    /**
     * Reset the cached static 'version' field to null before each test
     * so getVersion() re-evaluates each time.
     */
    @BeforeEach
    @AfterEach
    void resetVersionCache() throws Exception {
        Field versionField = VersionUtils.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(null, null);
    }

    // --- getVersionId ---

    @Test
    void getVersionId_v1_20_R1_returns1201() {
        assertThat(VersionUtils.v1_20_R1.getVersionId()).isEqualTo(1201);
    }

    @Test
    void getVersionId_v1_21_R11_returns12111() {
        assertThat(VersionUtils.v1_21_R11.getVersionId()).isEqualTo(12111);
    }

    @Test
    void getVersionId_v1_21_R1_returns1211() {
        assertThat(VersionUtils.v1_21_R1.getVersionId()).isEqualTo(1211);
    }

    // --- getVersion with valid Bukkit version ---

    @Test
    void getVersion_1_20_1_returnsV1_20_R1() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.20.1-R0.1-SNAPSHOT");

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_20_R1);
        }
    }

    @Test
    void getVersion_1_21_1_returnsV1_21_R1() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.1-R0.1-SNAPSHOT");

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_21_R1);
        }
    }

    @Test
    void getVersion_1_20_6_returnsV1_20_R6() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.20.6-R0.1-SNAPSHOT");

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_20_R6);
        }
    }

    // --- getVersion caches result ---

    @Test
    void getVersion_calledTwice_returnsSameInstance() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.1-R0.1-SNAPSHOT");

            VersionUtils first = VersionUtils.getVersion();
            VersionUtils second = VersionUtils.getVersion();

            assertThat(first).isSameAs(second);
        }
    }

    // --- getVersion with unknown version falls back to latest ---

    @Test
    void getVersion_unknownVersion_fallsToLatest() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("9.99.9-R0.1-SNAPSHOT");
            logUtil.when(() -> LogUtil.error(anyString(), any(), any())).thenAnswer(inv -> null);

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_21_R11);
        }
    }

    @Test
    void getVersion_nullBukkitVersion_fallsToLatest() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn(null);
            logUtil.when(() -> LogUtil.error(anyString(), any(), any())).thenAnswer(inv -> null);

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_21_R11);
        }
    }

    // --- isAtLeastVersion ---

    @Test
    void isAtLeastVersion_sameVersion_returnsTrue() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.1-R0.1-SNAPSHOT");

            assertThat(VersionUtils.isAtLeastVersion(VersionUtils.v1_21_R1)).isTrue();
        }
    }

    @Test
    void isAtLeastVersion_newerServer_returnsTrue() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.1-R0.1-SNAPSHOT");

            assertThat(VersionUtils.isAtLeastVersion(VersionUtils.v1_20_R1)).isTrue();
        }
    }

    @Test
    void isAtLeastVersion_olderServer_returnsFalse() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.20.1-R0.1-SNAPSHOT");

            assertThat(VersionUtils.isAtLeastVersion(VersionUtils.v1_21_R11)).isFalse();
        }
    }

    // --- isNewerOrEqualsTo ---

    @Test
    void isNewerOrEqualsTo_sameVersion_returnsTrue() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.20.1-R0.1-SNAPSHOT");

            assertThat(VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R1)).isTrue();
        }
    }

    @Test
    void isNewerOrEqualsTo_olderServer_returnsFalse() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.20.1-R0.1-SNAPSHOT");

            assertThat(VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_21_R11)).isFalse();
        }
    }

    // --- Alternate versionId matching ---

    @Test
    void getVersion_1_20_matchesV1_20_R1_viaAlternateId() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // "1.20" → replaceAll("\\.", "") → "120" matches the alternate id 120
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.20-R0.1-SNAPSHOT");

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_20_R1);
        }
    }

    @Test
    void getVersion_1_21_matchesV1_21_R1_viaAlternateId() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21-R0.1-SNAPSHOT");

            VersionUtils result = VersionUtils.getVersion();

            assertThat(result).isEqualTo(VersionUtils.v1_21_R1);
        }
    }
}
