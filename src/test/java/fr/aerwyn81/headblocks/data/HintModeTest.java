package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.services.LanguageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HintModeTest {

    @Mock
    LanguageService languageService;

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
        when(languageService.message("Other.Sound")).thenReturn("Son");

        assertThat(HintMode.SOUND.getLocalizedName(languageService)).isEqualTo("Son");
    }

    @Test
    void getLocalizedName_ACTIONBAR_callsLanguageService() {
        when(languageService.message("Other.ActionBar")).thenReturn("Barre d'action");

        assertThat(HintMode.ACTIONBAR.getLocalizedName(languageService)).isEqualTo("Barre d'action");
    }
}
