package com.cheatkey.module.auth.domain.validate;


import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class NicknameValidator {

    public void checkFormat(String nickname) {
        if (nickname.length() < 2 || nickname.length() > 5) {
            throw new CustomException(ErrorCode.AUTH_INVALID_NICKNAME_LENGTH);
        }

        if (containsEmoji(nickname)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_NICKNAME_EMOJI);
        }
    }

    private boolean containsEmoji(String input) {
        return input.codePoints().anyMatch(codepoint ->
                Character.getType(codepoint) == Character.SURROGATE ||
                        Character.getType(codepoint) == Character.OTHER_SYMBOL
        );
    }
}