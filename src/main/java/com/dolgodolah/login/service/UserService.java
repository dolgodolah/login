package com.dolgodolah.login.service;

import com.dolgodolah.login.domain.User;
import com.dolgodolah.login.dto.UserForm;
import com.dolgodolah.login.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if(user!=null){
            return user;
        }
        throw new UsernameNotFoundException("User '" + username + "' Not Found");
    }

    public Long join(User user){
        validateDuplicateUser(user);
        return userRepository.save(user).getId();
    }

    private void validateDuplicateUser(User user){
        userRepository.findByEmail(user.getEmail());
        if(userRepository.findByEmail(user.getEmail())!=null){
            throw new IllegalStateException("이미 가입한 이메일입니다.");
        }
    }

    /**
     * 유저 정보를 수정합니다.
     * @param userForm 폼에 입력한 유저 정보
     * @return 유저 ID
     */
    public Long updateUserInfo(UserForm userForm){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        user.setName(userForm.getName());

        // 비밀번호 변경 감지
        if(!userForm.getPassword().isEmpty()){
            user.setPassword(passwordEncoder.encode(userForm.getPassword()));
        }
        return userRepository.save(user).getId();
    }
}
