package fr.aerwyn81.headblocks.services;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatPromptServiceTest {

    @Mock
    Player player;

    private ChatPromptService service;
    private AtomicReference<String> answer;
    private AtomicBoolean cancelled;

    @BeforeEach
    void setUp() {
        service = new ChatPromptService();
        answer = new AtomicReference<>();
        cancelled = new AtomicBoolean(false);
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private void prompt() {
        service.prompt(player, "type something", answer::set, p -> cancelled.set(true));
    }

    @Test
    void prompt_closesTheMenuAndSendsTheQuestion() {
        prompt();

        verify(player).closeInventory();
        verify(player).sendMessage("type something");
        assertThat(service.hasPending(player)).isTrue();
    }

    @Test
    void prompt_blankMessage_sendsNothing() {
        service.prompt(player, "  ", answer::set, p -> cancelled.set(true));

        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void process_deliversTheTrimmedAnswer() {
        prompt();

        service.process(player, "  hb.vip  ");

        assertThat(answer.get()).isEqualTo("hb.vip");
        assertThat(cancelled).isFalse();
        assertThat(service.hasPending(player)).isFalse();
    }

    @Test
    void process_cancelKeyword_runsTheCancelBranch() {
        prompt();

        service.process(player, "CANCEL");

        assertThat(cancelled).isTrue();
        assertThat(answer.get()).isNull();
    }

    @Test
    void process_nullMessage_runsTheCancelBranch() {
        prompt();

        service.process(player, null);

        assertThat(cancelled).isTrue();
    }

    @Test
    void process_withoutPendingPrompt_doesNothing() {
        service.process(player, "hello");

        assertThat(answer.get()).isNull();
        assertThat(cancelled).isFalse();
    }

    @Test
    void process_onlyAnswersOnce() {
        prompt();

        service.process(player, "first");
        service.process(player, "second");

        assertThat(answer.get()).isEqualTo("first");
    }

    @Test
    void cancel_dropsThePendingPromptSilently() {
        prompt();

        service.cancel(player.getUniqueId());

        assertThat(service.hasPending(player)).isFalse();
        assertThat(cancelled).isFalse();
    }
}
