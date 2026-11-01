package com.editshare.dto;

import com.editshare.entity.EditMode;
import com.editshare.entity.ExpirationDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateClipRequest {

    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String language = "plaintext";

    private EditMode editMode = EditMode.PRIVATE;

    private String viewPassword;

    private String editPassword;

    @NotNull(message = "Expiration duration is mandatory")
    private ExpirationDuration expiration;

    public CreateClipRequest() {
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

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public String getViewPassword() {
        return viewPassword;
    }

    public void setViewPassword(String viewPassword) {
        this.viewPassword = viewPassword;
    }

    public String getEditPassword() {
        return editPassword;
    }

    public void setEditPassword(String editPassword) {
        this.editPassword = editPassword;
    }

    public ExpirationDuration getExpiration() {
        return expiration;
    }

    public void setExpiration(ExpirationDuration expiration) {
        this.expiration = expiration;
    }
}
