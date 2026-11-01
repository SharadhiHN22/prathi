package com.editshare.service;

import com.editshare.dto.ClipResponse;
import com.editshare.dto.CreateClipRequest;
import com.editshare.dto.UpdateClipRequest;
import com.editshare.entity.Clip;
import com.editshare.entity.ClipVersion;
import com.editshare.entity.EditMode;
import com.editshare.entity.ExpirationDuration;
import com.editshare.exception.ConflictException;
import com.editshare.exception.InvalidPasswordException;
import com.editshare.exception.ResourceNotFoundException;
import com.editshare.exception.UnauthorizedException;
import com.editshare.repository.ClipRepository;
import com.editshare.repository.ClipVersionRepository;
import com.editshare.util.SlugGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClipService {

    private final ClipRepository clipRepository;
    private final ClipVersionRepository clipVersionRepository;
    private final SlugGenerator slugGenerator;
    private final PasswordEncoder passwordEncoder;

    @Value("${editshare.app.base-url:http://localhost:8085}")
    private String baseUrl;

    public ClipService(ClipRepository clipRepository,
                       ClipVersionRepository clipVersionRepository,
                       SlugGenerator slugGenerator,
                       PasswordEncoder passwordEncoder) {
        this.clipRepository = clipRepository;
        this.clipVersionRepository = clipVersionRepository;
        this.slugGenerator = slugGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void purgeExpiredClips() {
        java.util.List<Clip> expiredClips = clipRepository.findByExpiresAtNotNullAndExpiresAtLessThanEqual(LocalDateTime.now());
        for (Clip expired : expiredClips) {
            clipVersionRepository.deleteByClipId(expired.getId());
            clipRepository.delete(expired);
        }
        if (!expiredClips.isEmpty()) {
            clipRepository.flush();
        }
    }

    @Transactional
    public ClipResponse createClip(CreateClipRequest request) {
        if (request.getExpiration() == null) {
            throw new IllegalArgumentException("Expiration duration is mandatory for all new clips");
        }

        // Clean up expired clips before deciding whether a slug is available
        purgeExpiredClips();

        // Generate normalized URL slug from clip title
        String rawTitle = request.getTitle() != null && !request.getTitle().trim().isEmpty() ? request.getTitle().trim() : "Untitled Clip";
        String slug = slugGenerator.toSlug(rawTitle);

        // Check whether that slug already belongs to an existing active clip
        if (clipRepository.existsBySlug(slug) || clipRepository.existsById(slug)) {
            throw new ConflictException("Clip name already exists. Please choose another name.");
        }

        // Internal ID (random 8 chars)
        String id = slugGenerator.generateSlug();

        // Generate secret edit token
        String rawEditToken = slugGenerator.generateSecretToken();
        String hashedEditToken = passwordEncoder.encode(rawEditToken);

        Clip clip = new Clip();
        clip.setId(id);
        clip.setSlug(slug);
        clip.setTitle(rawTitle);
        clip.setContent(request.getContent());
        clip.setLanguage(request.getLanguage() != null && !request.getLanguage().trim().isEmpty() ? request.getLanguage().trim() : "plaintext");
        clip.setEditMode(request.getEditMode() != null ? request.getEditMode() : EditMode.PRIVATE);
        clip.setEditTokenHash(hashedEditToken);

        if (request.getViewPassword() != null && !request.getViewPassword().trim().isEmpty()) {
            clip.setViewPasswordHash(passwordEncoder.encode(request.getViewPassword().trim()));
        }

        if (request.getEditPassword() != null && !request.getEditPassword().trim().isEmpty()) {
            clip.setEditPasswordHash(passwordEncoder.encode(request.getEditPassword().trim()));
        }

        // Calculate and enforce expiration
        clip.setExpiresAt(request.getExpiration().calculateExpiration(LocalDateTime.now()));
        clip.setDeleteAfterOneView(request.getExpiration() == ExpirationDuration.AFTER_ONE_VIEW);

        clip.setCurrentVersion(1);
        Clip savedClip = clipRepository.save(clip);

        // Record initial version 1
        ClipVersion initialVersion = new ClipVersion(
                savedClip,
                1,
                savedClip.getTitle(),
                savedClip.getContent(),
                savedClip.getLanguage(),
                "INITIAL"
        );
        clipVersionRepository.save(initialVersion);

        ClipResponse response = mapToResponse(savedClip, true);
        response.setEditToken(rawEditToken); // Only returned upon creation
        return response;
    }

    @Transactional(noRollbackFor = ResourceNotFoundException.class)
    public ClipResponse getClip(String id, String viewPassword, String editToken) {
        Clip clip = clipRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clip not found with ID: " + id));

        // Check if clip was configured for single view and already consumed
        if (Boolean.TRUE.equals(clip.getDeleteAfterOneView())) {
            if (clip.getViewCount() != null && clip.getViewCount() >= 1) {
                clipVersionRepository.deleteByClipId(clip.getId());
                clipRepository.delete(clip);
                clipRepository.flush();
                throw new ResourceNotFoundException("This clip has expired and is no longer available");
            }
        } else if (clip.isExpired()) {
            clipVersionRepository.deleteByClipId(clip.getId());
            clipRepository.delete(clip);
            clipRepository.flush();
            throw new ResourceNotFoundException("This clip has expired and is no longer available");
        }

        boolean isCreator = isCreatorTokenValid(clip, editToken);

        // Check password protection
        if (clip.isPasswordProtected() && !isCreator) {
            if (viewPassword == null || !passwordEncoder.matches(viewPassword, clip.getViewPasswordHash())) {
                throw new InvalidPasswordException("This clip is password protected");
            }
        }

        // Record first view for single-view clip
        if (Boolean.TRUE.equals(clip.getDeleteAfterOneView())) {
            clip.setViewCount(1);
            clip.setExpiresAt(LocalDateTime.now());
            clipRepository.saveAndFlush(clip);
        }

        boolean canEdit = isCreator;
        if (!canEdit && clip.getEditMode() == EditMode.COLLABORATIVE) {
            // If collaborative and has no edit password, anyone can edit
            canEdit = !clip.hasEditPassword();
        }

        return mapToResponse(clip, canEdit);
    }

    @Transactional
    public ClipResponse updateClip(String id, UpdateClipRequest request, String editToken, String editPassword) {
        Clip clip = clipRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clip not found with ID: " + id));

        if (clip.isExpired()) {
            clipVersionRepository.deleteByClipId(clip.getId());
            clipRepository.delete(clip);
            clipRepository.flush();
            throw new ResourceNotFoundException("This clip has expired and is no longer available");
        }

        // Version-based conflict detection to prevent silent overwriting
        if (request.getExpectedVersion() != null && !request.getExpectedVersion().equals(clip.getCurrentVersion())) {
            throw new ConflictException("Clip has been modified by another collaborator (current version: v"
                    + clip.getCurrentVersion() + "). Please refresh before editing.");
        }

        boolean isCreator = isCreatorTokenValid(clip, editToken);

        if (!isCreator) {
            if (clip.getEditMode() != EditMode.COLLABORATIVE) {
                throw new UnauthorizedException("Only the creator can edit this private clip");
            }
            if (clip.hasEditPassword()) {
                if (editPassword == null || !passwordEncoder.matches(editPassword, clip.getEditPasswordHash())) {
                    throw new UnauthorizedException("Collaborative edit password is required or invalid");
                }
            }
        }

        // Increment version number and update content
        int nextVersion = clip.getCurrentVersion() + 1;
        clip.setCurrentVersion(nextVersion);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            clip.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            clip.setContent(request.getContent());
        }
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            clip.setLanguage(request.getLanguage().trim());
        }

        // When editing, allow extending or updating the expiration time
        if (request.getExpiration() != null) {
            clip.setExpiresAt(request.getExpiration().calculateExpiration(LocalDateTime.now()));
            clip.setDeleteAfterOneView(request.getExpiration() == ExpirationDuration.AFTER_ONE_VIEW);
        }

        Clip updatedClip = clipRepository.save(clip);

        // Record history snapshot
        ClipVersion newVersion = new ClipVersion(
                updatedClip,
                nextVersion,
                updatedClip.getTitle(),
                updatedClip.getContent(),
                updatedClip.getLanguage(),
                "UPDATE"
        );
        clipVersionRepository.save(newVersion);

        return mapToResponse(updatedClip, true);
    }

    @Transactional
    public boolean verifyPassword(String id, String password) {
        Clip clip = findAndValidateActiveClip(id);
        if (!clip.isPasswordProtected()) {
            return true;
        }
        if (password == null || !passwordEncoder.matches(password, clip.getViewPasswordHash())) {
            throw new InvalidPasswordException("Incorrect password");
        }
        return true;
    }

    @Transactional
    public void deleteClip(String id, String editToken) {
        Clip clip = findAndValidateActiveClip(id);
        if (!isCreatorTokenValid(clip, editToken)) {
            throw new UnauthorizedException("Only the creator can delete this clip");
        }
        clipVersionRepository.deleteByClipId(clip.getId());
        clipRepository.delete(clip);
    }

    @Transactional(noRollbackFor = ResourceNotFoundException.class)
    public Clip findAndValidateActiveClip(String id) {
        Clip clip = clipRepository.findByIdOrSlug(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clip not found with ID: " + id));

        if (clip.isExpired()) {
            // Delete associated version history and the clip itself
            clipVersionRepository.deleteByClipId(clip.getId());
            clipRepository.delete(clip);
            clipRepository.flush();
            throw new ResourceNotFoundException("This clip has expired and is no longer available");
        }

        return clip;
    }

    public boolean isCreatorTokenValid(Clip clip, String editToken) {
        if (editToken == null || editToken.trim().isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(editToken.trim(), clip.getEditTokenHash());
    }

    private ClipResponse mapToResponse(Clip clip, boolean canEdit) {
        ClipResponse response = new ClipResponse();
        response.setId(clip.getId());
        response.setSlug(clip.getSlug());
        response.setTitle(clip.getTitle());
        response.setContent(clip.getContent());
        response.setLanguage(clip.getLanguage());
        response.setEditMode(clip.getEditMode());
        response.setCurrentVersion(clip.getCurrentVersion());
        response.setCanEdit(canEdit);
        response.setIsPasswordProtected(clip.isPasswordProtected());
        response.setHasEditPassword(clip.hasEditPassword());
        response.setExpiresAt(clip.getExpiresAt());
        response.setDeleteAfterOneView(Boolean.TRUE.equals(clip.getDeleteAfterOneView()));
        response.setCreatedAt(clip.getCreatedAt());
        response.setUpdatedAt(clip.getUpdatedAt());
        String publicIdentifier = clip.getSlug() != null && !clip.getSlug().isEmpty() ? clip.getSlug() : clip.getId();
        response.setUrl(baseUrl + "/" + publicIdentifier);
        return response;
    }
}
