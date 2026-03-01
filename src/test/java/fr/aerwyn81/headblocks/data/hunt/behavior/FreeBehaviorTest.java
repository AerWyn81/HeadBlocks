package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class FreeBehaviorTest {

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    private final FreeBehavior behavior = new FreeBehavior();
    private final Hunt hunt = new Hunt("test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

    @Test
    void canPlayerClick_alwaysReturnsAllow() {
        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
        assertThat(result.denyMessage()).isNull();
    }

    @Test
    void onHeadFound_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> behavior.onHeadFound(player, headLocation, hunt));
    }

    @Test
    void getId_returnsFree() {
        assertThat(behavior.getId()).isEqualTo("free");
    }

    @Test
    void getDisplayInfo_returnsEmptyString() {
        assertThat(behavior.getDisplayInfo(player, hunt)).isEmpty();
    }
}
