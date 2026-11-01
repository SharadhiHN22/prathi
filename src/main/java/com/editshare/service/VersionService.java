package com.editshare.service;

import com.editshare.dto.ClipResponse;
import com.editshare.dto.UpdateClipRequest;
import com.editshare.dto.VersionResponse;
import com.editshare.entity.Clip;
import com.editshare.entity.ClipVersion;
import com.editshare.exception.InvalidPasswordException;
import com.editshare.exception.ResourceNotFoundException;
import com.editshare.repository.ClipVersionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VersionService {

    private final ClipVersionRepository clipVersionRepository;
    private final ClipService clipService;
    private final PasswordEncoder passwordEncoder;

    public VersionService(ClipVersionRepository clipVersionRepository,
                          ClipService clipService,
                          PasswordEncoder passwordEncoder) {
        this.clipVersionRepository = clipVersionRepository;
        this.clipService = clipService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(noRollbackFor = ResourceNotFoundException.class)
    public List<VersionResponse> getVersions(String clipId, String viewPassword) {
        Clip clip = clipService.findAndValidateActiveClip(clipId);
        validateViewAccess(clip, viewPassword);

        return clipVersionRepository.findByClipIdOrderByVersionNumberDesc(clipId)
                .stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(noRollbackFor = ResourceNotFoundException.class)
    public VersionResponse getVersion(String clipId, Integer versionNumber, String viewPassword) {
        Clip clip = clipService.findAndValidateActiveClip(clipId);
        validateViewAccess(clip, viewPassword);

        ClipVersion version = clipVersionRepository.findByClipIdAndVersionNumber(clipId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version " + versionNumber + " not found for clip " + clipId));

        return mapToVersionResponse(version);
    }

    @Transactional
    public ClipResponse revertVersion(String clipId, Integer versionNumber, String editToken, String editPassword) {
        // Find historical version to revert to
        ClipVersion targetVersion = clipVersionRepository.findByClipIdAndVersionNumber(clipId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version " + versionNumber + " not found for clip " + clipId));

        UpdateClipRequest updateReq = new UpdateClipRequest();
        updateReq.setTitle(targetVersion.getTitle());
        updateReq.setContent(targetVersion.getContent());
        updateReq.setLanguage(targetVersion.getLanguage());

        return clipService.updateClip(clipId, updateReq, editToken, editPassword);
    }

    private void validateViewAccess(Clip clip, String viewPassword) {
        if (clip.isPasswordProtected()) {
            if (viewPassword == null || !passwordEncoder.matches(viewPassword, clip.getViewPasswordHash())) {
                throw new InvalidPasswordException("This clip is password protected");
            }
        }
    }

    private VersionResponse mapToVersionResponse(ClipVersion v) {
        return new VersionResponse(
                v.getId(),
                v.getVersionNumber(),
                v.getTitle(),
                v.getContent(),
                v.getLanguage(),
                v.getEditType(),
                v.getCreatedAt()
        );
    }
}
