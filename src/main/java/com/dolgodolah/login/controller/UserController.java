package com.dolgodolah.login.controller;

import com.dolgodolah.login.domain.User;
import com.dolgodolah.login.dto.UserForm;
import com.dolgodolah.login.repository.UserRepository;
import com.dolgodolah.login.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;

@Controller
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String dispLogin(){
        return "login";
    }

    // 회원가입 폼
    @GetMapping("/signup")
    public String dispSignUp(Model model){
        model.addAttribute("userForm", new UserForm());
        return "signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String execSignUp(@Valid UserForm userForm, BindingResult result, Model model){
        // 유저 폼 입력에 대한 유효성 확인
        if(result.hasErrors()){
            return "signup";
        }
        try{
            User user = userForm.toEntity(passwordEncoder);
            userService.join(user);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }

        return "redirect:/login";
    }

    // 인증키 확인
    @GetMapping("authConfirm")
    public String signupConfirm(@RequestParam String email, @RequestParam String authKey, Model model){
        User user = userRepository.findByEmail(email);

        // 유효 시간이 만료되지 않은 채 인증키가 일치하면 유저의 인증상태를 갱신합니다.
        if(!user.authExpired()){
            if(user.getAuthKey().equals(authKey)){
                userService.updateAuthStatus(user);
                return "user/authSuccess";
            }
        }
        model.addAttribute("email", email);
        return "user/authFailure";

    }

    // 인증메일 전송
    @GetMapping("sendAuthEmail")
    public String sendAuthEmail(String email) throws MessagingException, UnsupportedEncodingException {
        User user = userRepository.findByEmail(email);
        userService.generateAuthKey(user);
        return "index";
    }

    // 내정보 수정 폼
    @GetMapping("/myinfo")
    public String dispMyInfo(Model model, @AuthenticationPrincipal User user){
        UserForm userForm = UserForm.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .name(user.getName())
                .build();
        model.addAttribute("userForm", userForm);

        User findUser = userRepository.getById(user.getId());
        model.addAttribute("test", findUser.getName());

        return "user/myinfo";
    }

    // 내정보 수정 처리
    @PostMapping("/myinfo")
    public String execMyInfo(UserForm userForm, @AuthenticationPrincipal User user){
        userService.updateUserInfo(userForm);
        return "redirect:/myinfo";
    }

    // 로그인 실패 처리
    @PostMapping("/loginFailure")
    public String dispLoginFailure(){
        return "user/loginFailure";
    }


    @GetMapping("/denied")
    public String dispDenied(){
        return "denied";
    }
}
