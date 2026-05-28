package com.dentruth.user.application;

public interface EmailSender {
    void send(String toEmail, String authCode);
}
