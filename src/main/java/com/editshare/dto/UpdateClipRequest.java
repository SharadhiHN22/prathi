package com.editshare.dto;

import com.editshare.entity.ExpirationDuration;
import jakarta.validation.constraints.NotBlank;

public class UpdateClipRequest {

    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String language;

    private ExpirationDuration expiration;

    private Integer expectedVersion;

    public UpdateClipRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public ExpirationDuration getExpiration() {
        return expiration;
    }

    public void setExpiration(ExpirationDuration expiration) {
        this.expiration = expiration;
    }

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
