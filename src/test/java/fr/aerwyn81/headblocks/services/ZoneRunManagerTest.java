package fr.aerwyn81.headblocks.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneRunManagerTest {

    @BeforeEach
    void setUp() {
        ZoneRunManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        ZoneRunManager.clearAll();
    }

    @Test
    void clearAllForHunt_removesEngagedAndReleasedForThatHunt() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        ZoneRunManager.engage(p1, "hunt-1");
        ZoneRunManager.markReleased(p2, "hunt-1");

        ZoneRunManager.clearAllForHunt("hunt-1");

        assertThat(ZoneRunManager.isEngaged(p1)).isFalse();
        assertThat(ZoneRunManager.isReleased(p2, "hunt-1")).isFalse();
    }

    @Test
    void clearAllForHunt_leavesOtherHuntsUntouched() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        ZoneRunManager.engage(p1, "hunt-1");
        ZoneRunManager.engage(p2, "hunt-2");
        ZoneRunManager.markReleased(p2, "hunt-2");

        ZoneRunManager.clearAllForHunt("hunt-1");

        assertThat(ZoneRunManager.isEngaged(p1)).isFalse();
        assertThat(ZoneRunManager.getEngaged(p2)).isEqualTo("hunt-2");
        assertThat(ZoneRunManager.isReleased(p2, "hunt-2")).isTrue();
    }

    @Test
    void clearAllForHunt_unknownHunt_noOp() {
        UUID player = UUID.randomUUID();
        ZoneRunManager.engage(player, "hunt-1");

        ZoneRunManager.clearAllForHunt("hunt-X");

        assertThat(ZoneRunManager.getEngaged(player)).isEqualTo("hunt-1");
    }
}