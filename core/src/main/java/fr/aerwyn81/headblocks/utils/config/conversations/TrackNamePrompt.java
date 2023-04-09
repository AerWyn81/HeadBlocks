package fr.aerwyn81.headblocks.utils.config.conversations;

import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.TrackService;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;

import javax.annotation.Nonnull;
import java.util.Objects;

public class TrackNamePrompt extends ValidatingPrompt {

    @Override
    @Nonnull
    public String getPromptText(@Nonnull  ConversationContext context) {
        return LanguageService.getMessage("Chat.Conversation.AskUserTrackName");
    }

    @Override
    protected boolean isInputValid(@Nonnull ConversationContext context, @Nonnull String input) {
        if (input.trim().length() == 0) {
            context.setSessionData("error", "0");
            return false;
        }

        if (TrackService.getTrackByName(input).isPresent()) {
            context.setSessionData("error", "1");
            return false;
        }

        if (input.trim().length() < 3) {
            context.setSessionData("error", "2");
            return false;
        }

        if (input.matches("^[0-9]*$")) {
            context.setSessionData("error", "3");
            return false;
        }

        return true;
    }

    @Override
    protected String getFailedValidationText(ConversationContext context, @Nonnull String invalidInput) {
        if (context.getSessionData("error") != null)
        {
            var error = (String) context.getSessionData("error");
            switch (Objects.requireNonNull(error)) {
                case "0":
                    return LanguageService.getMessage("Chat.Conversation.TrackNamePromptIncorrectEmpty");
                case "1":
                    return LanguageService.getMessage("Chat.Conversation.TrackNamePromptIncorrectAlreadyExist");
                case "2":
                    return LanguageService.getMessage("Chat.Conversation.TrackNamePromptIncorrectNotEnoughChar");
                case "3":
                    return LanguageService.getMessage("Chat.Conversation.TrackNamePromptIncorrectOnlyNumber");
            }
        }

        return "";
    }

    @Override
    protected Prompt acceptValidatedInput(@Nonnull ConversationContext context, @Nonnull String input) {
        context.setSessionData("track", input);
        return new TrackNameSetPrompt();
    }
}
