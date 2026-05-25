package com.dentruth.config.oauth.unlink;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Component
public class GoogleUserUnlink implements OAuth2UserUnlink {

    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    private final RestTemplate restTemplate;

    @Override
    public void unlink(String accessToken, String userEmail, HttpServletRequest request, HttpServletResponse response) {
        String url = GOOGLE_REVOKE_URL + "?token=" + accessToken;
        restTemplate.postForObject(url, null, String.class);
    }
}
