package com.editshare.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;

@Component
public class SlugGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SLUG_LENGTH = 8;
    private static final int TOKEN_LENGTH = 48;
    private final SecureRandom random = new SecureRandom();

    public String generateSlug() {
        return generateRandomString(SLUG_LENGTH);
    }

    public String generateSecretToken() {
        return generateRandomString(TOKEN_LENGTH);
    }

    public String toSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "untitled";
        }
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = normalized.toLowerCase().trim();
        slug = slug.replaceAll("[\\s_.]+", "-");
        slug = slug.replaceAll("[^a-z0-9-]", "");
        slug = slug.replaceAll("-{2,}", "-");
        slug = slug.replaceAll("^-+|-+$", "");

        if (slug.isEmpty()) {
            return "untitled";
        }
        return slug;
    }

    public String generateShortSuffix() {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}
