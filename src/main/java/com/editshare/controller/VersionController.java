package com.editshare.controller;

import com.editshare.dto.ApiResponse;
import com.editshare.dto.ClipResponse;
import com.editshare.dto.VersionResponse;
import com.editshare.service.VersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clips/{clipId}/versions")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VersionResponse>>> getVersions(
            @PathVariable String clipId,
            @RequestHeader(value = "X-View-Password", required = false) String viewPassword) {
        List<VersionResponse> versions = versionService.getVersions(clipId, viewPassword);
        return ResponseEntity.ok(ApiResponse.success("Version history retrieved", versions));
    }

    @GetMapping("/{versionNumber}")
    public ResponseEntity<ApiResponse<VersionResponse>> getVersion(
            @PathVariable String clipId,
            @PathVariable Integer versionNumber,
            @RequestHeader(value = "X-View-Password", required = false) String viewPassword) {
        VersionResponse version = versionService.getVersion(clipId, versionNumber, viewPassword);
        return ResponseEntity.ok(ApiResponse.success("Version details retrieved", version));
    }

    @PostMapping("/{versionNumber}/revert")
    public ResponseEntity<ApiResponse<ClipResponse>> revertVersion(
            @PathVariable String clipId,
            @PathVariable Integer versionNumber,
            @RequestHeader(value = "X-Edit-Token", required = false) String editToken,
            @RequestHeader(value = "X-Edit-Password", required = false) String editPassword) {
        ClipResponse response = versionService.revertVersion(clipId, versionNumber, editToken, editPassword);
        return ResponseEntity.ok(ApiResponse.success("Clip successfully reverted to version " + versionNumber, response));
    }
}
