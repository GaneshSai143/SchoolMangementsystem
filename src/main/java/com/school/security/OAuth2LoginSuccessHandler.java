package com.school.security;

import com.school.entity.User;
import com.school.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String provider = oauth2Token.getAuthorizedClientRegistrationId();
            
            // Get OAuth2User from authentication
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // Extract user information based on provider
            String email = extractEmail(provider, attributes);
            String name = extractName(provider, attributes);
            
            if (email == null || name == null) {
                throw new RuntimeException("Failed to extract user information from OAuth2 provider");
            }

            // Process the OAuth2 user
            User user = userService.processOAuth2User(email, name, provider);

            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);

            // Redirect to frontend with token
            String targetUrl = String.format("http://localhost:4200/oauth2/redirect?token=%s", jwt);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            // Log the error and redirect to error page
            logger.error("OAuth2 authentication failed", e);
            String errorUrl = String.format("http://localhost:4200/oauth2/error?error=%s", 
                e.getMessage().replace(" ", "+"));
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("email");
            case "facebook" -> (String) attributes.get("email");
            case "twitter" -> (String) attributes.get("email");
            case "instagram" -> (String) attributes.get("email");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    private String extractName(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("name");
            case "facebook" -> {
                Map<String, Object> nameMap = (Map<String, Object>) attributes.get("name");
                yield nameMap != null ? (String) nameMap.get("name") : null;
            }
            case "twitter" -> (String) attributes.get("name");
            case "instagram" -> (String) attributes.get("name");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }
} 