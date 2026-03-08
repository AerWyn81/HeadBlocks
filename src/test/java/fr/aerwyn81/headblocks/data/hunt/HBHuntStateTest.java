package fr.aerwyn81.headblocks.data.hunt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HBHuntStateTest {

    @Test
    void of_ACTIVE_returnsACTIVE() {
        assertThat(HuntState.of("ACTIVE")).isEqualTo(HuntState.ACTIVE);
    }

    @Test
    void of_lowercase_inactive_returnsINACTIVE() {
        assertThat(HuntState.of("inactive")).isEqualTo(HuntState.INACTIVE);
    }

    @Test
    void of_mixedCase_ArChIvEd_returnsARCHIVED() {
        assertThat(HuntState.of("ArChIvEd")).isEqualTo(HuntState.ARCHIVED);
    }

    @Test
    void of_null_returnsACTIVE_asDefault() {
        assertThat(HuntState.of(null)).isEqualTo(HuntState.ACTIVE);
    }

    @Test
    void of_bogusValue_returnsACTIVE_asDefault() {
        assertThat(HuntState.of("bogus")).isEqualTo(HuntState.ACTIVE);
    }
}
