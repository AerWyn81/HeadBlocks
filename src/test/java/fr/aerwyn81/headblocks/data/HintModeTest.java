package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.services.LanguageService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class HintModeTest {

    @Test
    void next_SOUND_returnsACTIONBAR() {
        assertThat(HintMode.SOUND.next()).isEqualTo(HintMode.ACTIONBAR);
    }

    @Test
    void next_ACTIONBAR_returnsSOUND() {
        assertThat(HintMode.ACTIONBAR.next()).isEqualTo(HintMode.SOUND);
    }

    @Test
    void getLocalizedName_SOUND_callsLanguageService() {
        try (MockedStatic<LanguageService> ls = mockStatic(LanguageService.class)) {
            ls.when(() -> LanguageService.getMessage("Other.Sound")).thenReturn("Son");

            assertThat(HintMode.SOUND.getLocalizedName()).isEqualTo("Son");
        }
    }

    @Test
    void getLocalizedName_ACTIONBAR_callsLanguageService() {
        try (MockedStatic<LanguageService> ls = mockStatic(LanguageService.class)) {
            ls.when(() -> LanguageService.getMessage("Other.ActionBar")).thenReturn("Barre d'action");

            assertThat(HintMode.ACTIONBAR.getLocalizedName()).isEqualTo("Barre d'action");
        }
    }
}
