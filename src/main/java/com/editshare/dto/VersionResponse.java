package com.editshare.dto;

import java.time.LocalDateTime;

public class VersionResponse {

    private Long id;
    private Integer versionNumber;
    private String title;
    private String content;
    private String language;
    private String editType;
    private LocalDateTime createdAt;

    public VersionResponse() {
    }

    public VersionResponse(Long id, Integer versionNumber, String title, String content, String language, String editType, LocalDateTime createdAt) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.title = title;
        this.content = content;
        this.language = language;
        this.editType = editType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
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

    public String getEditType() {
        return editType;
    }

    public void setEditType(String editType) {
        this.editType = editType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
