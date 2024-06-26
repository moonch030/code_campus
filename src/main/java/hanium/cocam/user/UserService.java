package hanium.cocam.user;

import hanium.cocam.jwt.JwtUtil;
import hanium.cocam.refresh.RefreshToken;
import hanium.cocam.refresh.RefreshTokenService;
import hanium.cocam.user.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ProfileRepository profileRepository;

    @Value("${jwt.secret}")
    private String secretKey;
    private Long expiredMs = 1000 * 60 * 60 * 8790L;  // 토큰 유효시간 1년(테스트용)

    public Object signup(SignupRequest request) {
        try {
            // 이메일 중복 검사
            isDuplicateUserId(request.getUserId());

            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            request.setPassword(encodedPassword);

            // 회원 저장
            userRepository.save(request.toEntity());
            User findUser = userRepository.findByUserId(request.getUserId()).orElseThrow(() -> new IllegalArgumentException("not found userid : " + request.getUserId()));
            return new UserResponse(findUser);
        } catch (IllegalArgumentException e) {
            // 중복된 이메일로 인한 예외 발생 시에는 그대로 전달
            return e.getMessage();
        } catch (Exception e) {
            // 그 외의 예외 발생 시에는 일반적인 오류 메시지 반환
            return "회원가입 중 오류가 발생했습니다. 오류내용 : " + e.getMessage();
        }
    }

    public Object addProfile(AddProfileRequest request) {
        try {
            Long userNo = request.getUserNo();
            User user = userRepository.findById(userNo).orElseThrow(() -> new NoSuchElementException("not found User"));
            isDuplicateUserNo(user);
            Profile savedUserProfile = profileRepository.save(request.toEntity(user));

            return new AddProfileResponse(savedUserProfile);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "프로필 등록 중 오류가 발생했습니다. 내용: " + e.getMessage();
        }
    }

    private void isDuplicateUserNo(User user) {
        Optional<Profile> findProfileUserNo = profileRepository.findByUser(user);
        if (findProfileUserNo.isPresent()) {
            throw new IllegalArgumentException("이미 프로필이 등록된 회원입니다.");
        }
    }

    private void isDuplicateUserId(String userId) {
        Optional<User> findUserId = userRepository.findByUserId(userId);
        if (findUserId.isPresent()) {
            throw new IllegalArgumentException("중복된 아이디 입니다.");
        }
    }

    public Object login(LoginRequest request) {
        String userId = request.getUserId();
        String password = request.getPassword();

        // 존재하는 사용자인지 확인
        Optional<User> findUser = userRepository.findByUserId(userId);

        // 사용자가 존재하고 비밀번호가 일치하는 경우에만 인증 성공
        if (findUser.isPresent() && passwordEncoder.matches(password, findUser.get().getPassword())) {
            String userName = findUser.get().getUserName();
            Long userNo = findUser.get().getUserNo();

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUserId());
            String accessToken = JwtUtil.createJwt(userId, userNo, secretKey, expiredMs);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getRefreshToken())
                    .expiryDate(JwtUtil.getExpirationDate(accessToken, secretKey))
                    .userNo(userNo)
                    .userId(userId)
                    .userName(userName)
                    .build();
        } else {
            return new LoginFailResponse("입력하신 아이디 또는 비밀번호를 확인해주세요.");
        }
    }

    public List<UserResponse> findAll() {
        List<UserResponse> users = userRepository.findAll().stream().map(UserResponse::new).toList();
        return users;
    }

    public UserResponse findUser(Long userNo) {
        User findUser = userRepository.findById(userNo).orElseThrow(() -> new IllegalArgumentException("not found "+ userNo));
        return UserResponse.builder()
                .user(findUser)
                .build();
    }

    public Optional<LoginResponse> refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = JwtUtil.createJwt(user.getUserId(), user.getUserNo(), secretKey, expiredMs);
                    return LoginResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(request.getRefreshToken())
                            .expiryDate(JwtUtil.getExpirationDate(accessToken, secretKey))
                            .userNo(user.getUserNo())
                            .userId(user.getUserId())
                            .build();
                });
    }
    public String logout(LogoutRequest request) {
        return refreshTokenService.deleteByToken(request.getRefreshToken());
    }
}
