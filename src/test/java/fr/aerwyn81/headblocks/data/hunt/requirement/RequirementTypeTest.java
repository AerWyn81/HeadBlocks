package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RequirementTypeTest {

    @Mock
    PluginProvider pluginProvider;

    @Test
    void fromId_resolvesEveryDeclaredType() {
        for (RequirementType type : RequirementType.values()) {
            assertThat(RequirementType.fromId(type.getId())).isEqualTo(type);
        }
    }

    @Test
    void fromId_isCaseInsensitive() {
        assertThat(RequirementType.fromId("AREA")).isEqualTo(RequirementType.AREA);
    }

    @Test
    void fromId_unknownOrNull_returnsNull() {
        assertThat(RequirementType.fromId("nope")).isNull();
        assertThat(RequirementType.fromId(null)).isNull();
    }

    @Test
    void everyTypeHasAnIconAndALangKey() {
        for (RequirementType type : RequirementType.values()) {
            assertThat(type.getIcon()).isNotNull();
            assertThat(type.getLangKey()).isNotBlank();
        }
    }

    @Test
    void placeholderType_needsPlaceholderApi() {
        lenient().when(pluginProvider.isPlaceholderApiActive()).thenReturn(false);

        assertThat(RequirementType.PLACEHOLDER.isAvailable(pluginProvider)).isFalse();

        lenient().when(pluginProvider.isPlaceholderApiActive()).thenReturn(true);

        assertThat(RequirementType.PLACEHOLDER.isAvailable(pluginProvider)).isTrue();
    }

    @Test
    void otherTypes_areAlwaysAvailable() {
        for (RequirementType type : RequirementType.values()) {
            if (type == RequirementType.PLACEHOLDER) {
                continue;
            }
            assertThat(type.isAvailable(pluginProvider)).isTrue();
        }
    }

    @Test
    void isAvailable_withoutPluginProvider_isFalse() {
        assertThat(RequirementType.AREA.isAvailable(null)).isFalse();
    }
}
