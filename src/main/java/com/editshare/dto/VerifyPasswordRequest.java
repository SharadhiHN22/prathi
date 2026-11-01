package com.editshare.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyPasswordRequest {

    @NotBlank(message = "Password cannot be empty")
    private String password;

    public VerifyPasswordRequest() {
    }

    public VerifyPasswordRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
