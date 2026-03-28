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
class HBHuntTest {

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    @Mock
    ConfigService configService;

    @Test
    void constructor_hasFreeBehaviorByDefault() {
        HBHunt hunt = new HBHunt(configService, "test", "Test Hunt", HuntState.ACTIVE, 1, "DIAMOND");

        assertThat(hunt.getBehaviors()).hasSize(1);
        assertThat(hunt.getBehaviors().get(0)).isInstanceOf(FreeBehavior.class);
    }

    @Test
    void constructor_headsAreEmpty() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        assertThat(hunt.getHeadCount()).isZero();
        assertThat(hunt.getHeadUUIDs()).isEmpty();
    }

    @Test
    void constructor_stateIsCorrect() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.INACTIVE, 1, "DIAMOND");

        assertThat(hunt.getState()).isEqualTo(HuntState.INACTIVE);
    }

    @Test
    void isActive_trueForACTIVE() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        assertThat(hunt.isActive()).isTrue();
    }

    @Test
    void isActive_falseForINACTIVE() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.INACTIVE, 1, "DIAMOND");
        assertThat(hunt.isActive()).isFalse();
    }

    @Test
    void isActive_falseForARCHIVED() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ARCHIVED, 1, "DIAMOND");
        assertThat(hunt.isActive()).isFalse();
    }

    @Test
    void isDefault_trueForDefaultId() {
        HBHunt hunt = new HBHunt(configService, "default", "Default", HuntState.ACTIVE, 1, "DIAMOND");
        assertThat(hunt.isDefault()).isTrue();
    }

    @Test
    void isDefault_falseForOtherId() {
        HBHunt hunt = new HBHunt(configService, "custom", "Custom", HuntState.ACTIVE, 1, "DIAMOND");
        assertThat(hunt.isDefault()).isFalse();
    }

    @Test
    void headManagement_addContainsRemoveClearCount() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
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
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.addHead(UUID.randomUUID());

        assertThatThrownBy(() -> hunt.getHeadUUIDs().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setBehaviors_null_resetsToFreeBehavior() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.setBehaviors(null);

        assertThat(hunt.getBehaviors()).hasSize(1);
        assertThat(hunt.getBehaviors().get(0)).isInstanceOf(FreeBehavior.class);
    }

    @Test
    void setBehaviors_emptyList_resetsToFreeBehavior() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.setBehaviors(Collections.emptyList());

        assertThat(hunt.getBehaviors()).hasSize(1);
        assertThat(hunt.getBehaviors().get(0)).isInstanceOf(FreeBehavior.class);
    }

    @Test
    void evaluateBehaviors_allAllow_returnsAllow() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

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
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

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
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

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
    void evaluateAccessGates_onlyChecksAccessGateBehaviors() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior gate = mock(Behavior.class);
        Behavior normal = mock(Behavior.class);
        when(gate.isAccessGate()).thenReturn(true);
        when(gate.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.allow());
        when(normal.isAccessGate()).thenReturn(false);
        hunt.setBehaviors(List.of(gate, normal));

        BehaviorResult result = hunt.evaluateAccessGates(player, headLocation);

        assertThat(result.allowed()).isTrue();
        verify(gate).canPlayerClick(player, headLocation, hunt);
        verify(normal, never()).canPlayerClick(any(), any(), any());
    }

    @Test
    void evaluateAccessGates_gateDeny_returnsDenyWithoutCheckingOthers() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior gate = mock(Behavior.class);
        Behavior normal = mock(Behavior.class);
        when(gate.isAccessGate()).thenReturn(true);
        when(gate.canPlayerClick(player, headLocation, hunt)).thenReturn(BehaviorResult.deny("hunt ended"));
        hunt.setBehaviors(List.of(gate, normal));

        BehaviorResult result = hunt.evaluateAccessGates(player, headLocation);

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isEqualTo("hunt ended");
        verify(normal, never()).canPlayerClick(any(), any(), any());
    }

    @Test
    void evaluateAccessGates_noGates_returnsAllow() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior normal = mock(Behavior.class);
        when(normal.isAccessGate()).thenReturn(false);
        hunt.setBehaviors(List.of(normal));

        BehaviorResult result = hunt.evaluateAccessGates(player, headLocation);

        assertThat(result.allowed()).isTrue();
        verify(normal, never()).canPlayerClick(any(), any(), any());
    }

    @Test
    void notifyHeadFound_callsOnHeadFoundOnAllBehaviors() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");

        Behavior b1 = mock(Behavior.class);
        Behavior b2 = mock(Behavior.class);
        hunt.setBehaviors(List.of(b1, b2));

        hunt.notifyHeadFound(player, headLocation);

        verify(b1).onHeadFound(player, headLocation, hunt);
        verify(b2).onHeadFound(player, headLocation, hunt);
    }

    @Test
    void setConfig_null_createsDefaultHuntConfig() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        hunt.setConfig(null);

        assertThat(hunt.getConfig()).isNotNull();
    }

    @Test
    void equals_sameId_areEqual() {
        HBHunt h1 = new HBHunt(configService, "abc", "Hunt A", HuntState.ACTIVE, 1, "D");
        HBHunt h2 = new HBHunt(configService, "abc", "Hunt B", HuntState.INACTIVE, 2, "G");

        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void equals_differentId_areNotEqual() {
        HBHunt h1 = new HBHunt(configService, "abc", "Hunt", HuntState.ACTIVE, 1, "D");
        HBHunt h2 = new HBHunt(configService, "xyz", "Hunt", HuntState.ACTIVE, 1, "D");

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void addHead_duplicateUUID_isIdempotent() {
        HBHunt hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
        UUID head = UUID.randomUUID();

        hunt.addHead(head);
        hunt.addHead(head); // same UUID again

        assertThat(hunt.getHeadCount()).isEqualTo(1);
    }

    @Test
    void hashCode_sameId_sameHash() {
        HBHunt h1 = new HBHunt(configService, "abc", "A", HuntState.ACTIVE, 1, "D");
        HBHunt h2 = new HBHunt(configService, "abc", "B", HuntState.INACTIVE, 2, "G");

        assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
    }
}
