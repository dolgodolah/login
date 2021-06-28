package com.dolgodolah.login.dto;

import com.dolgodolah.login.domain.Role;
import com.dolgodolah.login.domain.User;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.validation.constraints.NotEmpty;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserForm {
    private Long id;

    @NotEmpty(message = "이메일을 입력해주세요.")
    private String email;

    @NotEmpty(message = "비밀번호를 입력해주세요.")
    private String password;
    private String name;

    public User toEntity(PasswordEncoder passwordEncoder){
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(Role.GUEST)
                .build();
    }
}
