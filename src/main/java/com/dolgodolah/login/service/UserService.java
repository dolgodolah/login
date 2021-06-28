package com.dolgodolah.login.service;

import com.dolgodolah.login.domain.Role;
import com.dolgodolah.login.domain.User;
import com.dolgodolah.login.dto.UserForm;
import com.dolgodolah.login.repository.UserRepository;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender javaMailSender;


    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender javaMailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.javaMailSender = javaMailSender;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if(user!=null){
            return user;
        }
        throw new UsernameNotFoundException("User '" + username + "' Not Found");
    }

    /**
     * 중복된 유저가 없을 경우 회원가입과 인증키 발송이 진행됩니다.
     * @param user 회원가입 폼에서 입력한 유저 정보
     * @return 유저 ID
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public Long join(User user) throws MessagingException, UnsupportedEncodingException {
        validateDuplicateUser(user);
        generateAuthKey(user);
        return userRepository.save(user).getId();
    }

    /**
     * 해당 유저의 이메일이 이미 가입되어있는지 확인합니다.
     * @param user 회원가입 폼에서 입력한 유저 정보
     */
    private void validateDuplicateUser(User user){
        userRepository.findByEmail(user.getEmail());
        if(userRepository.findByEmail(user.getEmail())!=null){
            throw new IllegalStateException("이미 가입한 이메일입니다.");
        }
    }

    /**
     * 8자리 인증키를 만들고 해당 유저의 인증 내용을 설정 후 메일을 전송합니다.
     * @param user
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    public void generateAuthKey(User user) throws UnsupportedEncodingException, MessagingException {
        // 8자 인증키 생성
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String authKey = RandomString.make(8);

        // 해당 유저의 인증키와 인증요청시간 설정
        user.setAuthKey(authKey);
        user.setAuthRequestedTime(new Date());
        userRepository.save(user);

        // 메일 전송
        sendAuthEmail(user,authKey);
    }

    /**
     * 인증메일을 구성하여 해당 유저의 이메일로 메일을 전송합니다.
     * @param user 회원가입 폼에서 입력한 유저 정보
     * @param authKey generateAuthKey()에서 생성된 인증키
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    public void sendAuthEmail(User user, String authKey) throws UnsupportedEncodingException, MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("dolgodolah@gmail.com", "dolgodolah");
        helper.setTo(user.getEmail());

        String subject = "회원가입 이메일 인증";

        String content = "<p>안녕하세요, " + user.getName() + "님!</p>"
                + "<p>아래 링크를 클릭하시면 이메일 인증이 완료됩니다.</p>"
                + "<a href='http://localhost:8080/authConfirm?email="
                + user.getEmail()
                + "&authKey="
                + user.getAuthKey()
                + "' target='_blank'>이메일 인증 확인</a>";
        helper.setSubject(subject);
        helper.setText(content,true);
        javaMailSender.send(message);
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

    public Long updateAuthStatus(User user){
        user.setRole(Role.USER);
        return userRepository.save(user).getId();
    }
}
