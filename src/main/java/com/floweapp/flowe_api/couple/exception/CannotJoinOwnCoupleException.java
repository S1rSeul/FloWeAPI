package com.floweapp.flowe_api.couple.exception;

public class CannotJoinOwnCoupleException extends RuntimeException {

  public CannotJoinOwnCoupleException() {
    super("Вы не можете присоединиться к своей же паре");
  }

  public CannotJoinOwnCoupleException(String message) {
    super(message);
  }
}
