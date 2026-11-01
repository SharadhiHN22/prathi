package com.editshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clips", indexes = {
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
public class Clip {

    @Id
    @Column(length = 16, nullable = false, updatable = false)
    private String id;

    @Column(name = "slug", unique = true, length = 150)
    private String slug;

    @Column(nullable = false)
    private String title = "Untitled Clip";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50, nullable = false)
    private String language = "plaintext";

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_mode", length = 20, nullable = false)
    private EditMode editMode = EditMode.PRIVATE;

    @Column(name = "edit_token_hash", nullable = false)
    private String editTokenHash;

    @Column(name = "view_password_hash")
    private String viewPasswordHash;

    @Column(name = "edit_password_hash")
    private String editPasswordHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "delete_after_one_view", nullable = false)
    private Boolean deleteAfterOneView = false;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "clip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClipVersion> versions = new ArrayList<>();

    public Clip() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.currentVersion == null) {
            this.currentVersion = 1;
        }
        if (this.title == null || this.title.trim().isEmpty()) {
            this.title = "Untitled Clip";
        }
        if (this.language == null || this.language.trim().isEmpty()) {
            this.language = "plaintext";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPasswordProtected() {
        return viewPasswordHash != null && !viewPasswordHash.trim().isEmpty();
    }

    public boolean hasEditPassword() {
        return editPasswordHash != null && !editPasswordHash.trim().isEmpty();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public String getEditTokenHash() {
        return editTokenHash;
    }

    public void setEditTokenHash(String editTokenHash) {
        this.editTokenHash = editTokenHash;
    }

    public String getViewPasswordHash() {
        return viewPasswordHash;
    }

    public void setViewPasswordHash(String viewPasswordHash) {
        this.viewPasswordHash = viewPasswordHash;
    }

    public String getEditPasswordHash() {
        return editPasswordHash;
    }

    public void setEditPasswordHash(String editPasswordHash) {
        this.editPasswordHash = editPasswordHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getDeleteAfterOneView() {
        return deleteAfterOneView != null ? deleteAfterOneView : false;
    }

    public void setDeleteAfterOneView(Boolean deleteAfterOneView) {
        this.deleteAfterOneView = deleteAfterOneView != null ? deleteAfterOneView : false;
    }

    public Integer getViewCount() {
        return viewCount != null ? viewCount : 0;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount != null ? viewCount : 0;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ClipVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<ClipVersion> versions) {
        this.versions = versions;
    }
}
