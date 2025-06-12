package com.school.security;

import com.school.entity.User;
import com.school.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        
        try {
            String email = getEmailFromOAuth2User(provider, oauth2User);
            String name = getNameFromOAuth2User(provider, oauth2User);
            
            // Process the OAuth2 user
            User user = userService.processOAuth2User(email, name, provider);
            
            return new CustomOAuth2User(user, oauth2User.getAttributes());
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private String getEmailFromOAuth2User(String provider, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("email");
            case "facebook" -> (String) attributes.get("email");
            case "twitter" -> (String) attributes.get("email");
            case "instagram" -> (String) attributes.get("email");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    private String getNameFromOAuth2User(String provider, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("name");
            case "facebook" -> (String) attributes.get("name");
            case "twitter" -> (String) attributes.get("name");
            case "instagram" -> (String) attributes.get("name");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }
} 