package com.busnow.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 *
 * @param username 로그인 아이디 (Users.Username, 4~50자)
 * @param password 비밀번호 (최소 8자, 서비스 계층에서 BCrypt 인코딩)
 * @param email    이메일 (Users.Email, 형식 검증)
 */
public record RegisterRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 50, message = "아이디는 4~50자 사이여야 합니다.")
        @JsonProperty("username")
        String username,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        @JsonProperty("password")
        String password,

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @JsonProperty("email")
        String email
) {}
