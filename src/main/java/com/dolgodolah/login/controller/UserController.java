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
import org.springframework.web.bind.support.SessionStatus;

import javax.validation.Valid;

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

    @GetMapping("/signup")
    public String dispSignUp(Model model){
        model.addAttribute("userForm", new UserForm());
        return "signup";
    }

    @PostMapping("/signup")
    public String execSignUp(@Valid UserForm userForm, BindingResult result, Model model){
        if(result.hasErrors()){
            return "signup";
        }
        try{
            User user = userForm.toEntity(passwordEncoder);
            userService.join(user);
        }catch (Exception e){
            model.addAttribute("error", e.getMessage());
            return "signup";
        }

        return "redirect:/login";
    }

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

    @PostMapping("/myinfo")
    public String execMyInfo(UserForm userForm, @AuthenticationPrincipal User user){
        userService.updateUserInfo(userForm);
        return "redirect:/myinfo";
    }
}
