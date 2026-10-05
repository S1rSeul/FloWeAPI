package com.floweapp.flowe_api.couple.exception;

public class InviteCodeNotFoundException extends RuntimeException {

  public InviteCodeNotFoundException() {
    super("Invite-код не найден");
  }

  public InviteCodeNotFoundException(String message) {
    super(message);
  }
}
