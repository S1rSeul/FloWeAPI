package com.floweapp.flowe_api.couple.exception;

public class InviteCodeGenerationException extends RuntimeException {
    public InviteCodeGenerationException(String message) {
        super(message);
    }

    public InviteCodeGenerationException() {
        super("Ошибка генерации кода, пожалуйста попробуйте еще раз");
    }
}
