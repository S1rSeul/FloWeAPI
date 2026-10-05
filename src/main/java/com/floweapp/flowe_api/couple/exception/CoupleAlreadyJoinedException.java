package com.floweapp.flowe_api.couple.exception;

public class CoupleAlreadyJoinedException extends RuntimeException {

    public CoupleAlreadyJoinedException() {
        super("В этой паре уже есть партнер");
    }

    public CoupleAlreadyJoinedException(String message) {
        super(message);
    }
}
