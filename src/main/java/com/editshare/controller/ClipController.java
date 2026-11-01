package com.editshare.controller;

import com.editshare.dto.*;
import com.editshare.service.ClipService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clips")
public class ClipController {

    private final ClipService clipService;

    public ClipController(ClipService clipService) {
        this.clipService = clipService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClipResponse>> createClip(@Valid @RequestBody CreateClipRequest request) {
        ClipResponse response = clipService.createClip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Clip created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClipResponse>> getClip(
            @PathVariable String id,
            @RequestHeader(value = "X-View-Password", required = false) String viewPassword,
            @RequestHeader(value = "X-Edit-Token", required = false) String editToken) {
        ClipResponse response = clipService.getClip(id, viewPassword, editToken);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(ApiResponse.success("Clip retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClipResponse>> updateClip(
            @PathVariable String id,
            @Valid @RequestBody UpdateClipRequest request,
            @RequestHeader(value = "X-Edit-Token", required = false) String editToken,
            @RequestHeader(value = "X-Edit-Password", required = false) String editPassword) {
        ClipResponse response = clipService.updateClip(id, request, editToken, editPassword);
        return ResponseEntity.ok(ApiResponse.success("Clip updated successfully", response));
    }

    @PostMapping("/{id}/verify-password")
    public ResponseEntity<ApiResponse<Boolean>> verifyPassword(
            @PathVariable String id,
            @Valid @RequestBody VerifyPasswordRequest request) {
        boolean valid = clipService.verifyPassword(id, request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Password verified", valid));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClip(
            @PathVariable String id,
            @RequestHeader(value = "X-Edit-Token", required = false) String editToken) {
        clipService.deleteClip(id, editToken);
        return ResponseEntity.ok(ApiResponse.success("Clip deleted successfully"));
    }
}
