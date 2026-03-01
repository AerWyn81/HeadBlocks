package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.BehaviorResult;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HuntTest {

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    @Mock
    ConfigService configService;

    @Test
    void constructor_hasFreeBehaviorByDefault() {
        Hunt hunt = new Hunt(configService, "test", "Test Hunt", HuntState.ACTIVE, 1, "DIAMOND");

        assertThat(hunt.getBehaviors()).hasSize(1);
        assertThat(hunt.getBehaviors().get(0)).isInstanceOf(FreeBehavior.class);
    }

    @Test
    void constructor_headsAreEmpty() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        assertThat(hunt.getHeadCount()).isZero();
        assertThat(hunt.getHeadUUIDs()).isEmpty();
    }

    @Test
    void constructor_stateIsCorrect() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.INACTIVE, 1, "DIAMOND");

        assertThat(hunt.getState()).isEqualTo(HuntState.INACTIVE);
    }

    @Test
    void isActive_trueForACTIVE() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        assertThat(hunt.isActive()).isTrue();
    }

    @Test
    void isActive_falseForINACTIVE() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.INACTIVE, 1, "DIAMOND");
        assertThat(hunt.isActive()).isFalse();
    }

    @Test
    void isActive_falseForARCHIVED() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ARCHIVED, 1, "DIAMOND");
        assertThat(hunt.isActive()).isFalse();
    }

    @Test
    void isDefault_trueForDefaultId() {
        Hunt hunt = new Hunt(configService, "default", "Default", HuntState.ACTIVE, 1, "DIAMOND");
        assertThat(hunt.isDefault()).isTrue();
    }

    @Test
    void isDefault_falseForOtherId() {
        Hunt hunt = new Hunt(configService, "custom", "Custom", HuntState.ACTIVE, 1, "DIAMOND");
        assertThat(hunt.isDefault()).isFalse();
    }

    @Test
    void headManagement_addContainsRemoveClearCount() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();

        hunt.addHead(h1);
        hunt.addHead(h2);

        assertThat(hunt.containsHead(h1)).isTrue();
        assertThat(hunt.getHeadCount()).isEqualTo(2);

        hunt.removeHead(h1);
        assertThat(hunt.containsHead(h1)).isFalse();
        assertThat(hunt.getHeadCount()).isEqualTo(1);

        hunt.clearHeads();
        assertThat(hunt.getHeadCount()).isZero();
    }

    @Test
    void getHeadUUIDs_returnsUnmodifiableSet() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.addHead(UUID.randomUUID());

        assertThatThrownBy(() -> hunt.getHeadUUIDs().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setBehaviors_null_resetsToFreeBehavior() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.setBehaviors(null);

        assertThat(hunt.getBehaviors()).hasSize(1);
        assertThat(hunt.getBehaviors().get(0)).isInstanceOf(FreeBehavior.class);
    }

    @Test
    void setBehaviors_emptyList_resetsToFreeBehavior() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.setBehaviors(Collections.emptyList());

        assertThat(hunt.getBehaviors()).hasSize(1);
        assertThat(hunt.getBehaviors().get(0)).isInstanceOf(FreeBehavior.class);
    }

    @Test
    void evaluateBehaviors_allAllow_returnsAllow() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior b1 = mock(Behavior.class);
        Behavior b2 = mock(Behavior.class);
        when(b1.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.allow());
        when(b2.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.allow());
        hunt.setBehaviors(List.of(b1, b2));

        BehaviorResult result = hunt.evaluateBehaviors(player, headLocation);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void evaluateBehaviors_firstDeny_returnsDenyAndSkipsSecond() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior b1 = mock(Behavior.class);
        Behavior b2 = mock(Behavior.class);
        when(b1.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.deny("blocked"));
        hunt.setBehaviors(List.of(b1, b2));

        BehaviorResult result = hunt.evaluateBehaviors(player, headLocation);

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isEqualTo("blocked");
        verify(b2, never()).canPlayerClick(any(), any(), any());
    }

    @Test
    void evaluateBehaviors_firstAllowSecondDeny_returnsDeny() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior b1 = mock(Behavior.class);
        Behavior b2 = mock(Behavior.class);
        when(b1.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.allow());
        when(b2.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.deny("second deny"));
        hunt.setBehaviors(List.of(b1, b2));

        BehaviorResult result = hunt.evaluateBehaviors(player, headLocation);

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isEqualTo("second deny");
    }

    @Test
    void notifyHeadFound_callsOnHeadFoundOnAllBehaviors() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior b1 = mock(Behavior.class);
        Behavior b2 = mock(Behavior.class);
        hunt.setBehaviors(List.of(b1, b2));

        hunt.notifyHeadFound(player, headLocation);

        verify(b1).onHeadFound(player, headLocation, hunt);
        verify(b2).onHeadFound(player, headLocation, hunt);
    }

    @Test
    void setConfig_null_createsDefaultHuntConfig() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.setConfig(null);

        assertThat(hunt.getConfig()).isNotNull();
    }

    @Test
    void equals_sameId_areEqual() {
        Hunt h1 = new Hunt(configService, "abc", "Hunt A", HuntState.ACTIVE, 1, "D");
        Hunt h2 = new Hunt(configService, "abc", "Hunt B", HuntState.INACTIVE, 2, "G");

        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void equals_differentId_areNotEqual() {
        Hunt h1 = new Hunt(configService, "abc", "Hunt", HuntState.ACTIVE, 1, "D");
        Hunt h2 = new Hunt(configService, "xyz", "Hunt", HuntState.ACTIVE, 1, "D");

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void addHead_duplicateUUID_isIdempotent() {
        Hunt hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        UUID head = UUID.randomUUID();

        hunt.addHead(head);
        hunt.addHead(head); // same UUID again

        assertThat(hunt.getHeadCount()).isEqualTo(1);
    }

    @Test
    void hashCode_sameId_sameHash() {
        Hunt h1 = new Hunt(configService, "abc", "A", HuntState.ACTIVE, 1, "D");
        Hunt h2 = new Hunt(configService, "abc", "B", HuntState.INACTIVE, 2, "G");

        assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
    }
}
