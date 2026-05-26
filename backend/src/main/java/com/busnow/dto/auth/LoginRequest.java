package com.busnow.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 요청 DTO.
 *
 * @param username 로그인 아이디 (Users.Username)
 * @param password 평문 비밀번호 (BCrypt 비교용)
 */
public record LoginRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @JsonProperty("username")
        String username,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @JsonProperty("password")
        String password
) {}
