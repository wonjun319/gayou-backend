package com.gayou.auth.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayou.auth.dto.LoginResponse;
import com.gayou.auth.dto.UserDto;
import com.gayou.auth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // application.properties 파일에서 Kakao REST API 키를 주입받음
    @Value("${kakao.rest.api.key}")
    private String KAKAO_REST_API_KEY;

    // UserService 객체를 주입받아 사용
    private final UserService userService;

    // 생성자를 통한 의존성 주입
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 로그인 엔드포인트
     * 
     * @param userDto - 사용자의 로그인 정보 (username, password)
     * @return JWT 토큰이 담긴 ResponseEntity
     * 
     *         사용자의 로그인 요청을 처리하고, 인증에 성공하면 JWT 토큰을 발급하여 반환합니다.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto userDto) {
        // 로그인 처리 후 JWT 토큰 발급
        LoginResponse response = userService.authenticate(userDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 가입 엔드포인트
     * 
     * @param userDto - 회원 가입할 사용자의 정보 (username, password, name, email,
     *                phoneNumber, birthday)
     * @return 성공 메시지가 담긴 ResponseEntity
     * 
     *         새로운 사용자를 데이터베이스에 저장합니다. 중복된 username이 있을 경우 예외가 발생합니다.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        // 회원 가입 처리
        userService.register(userDto);
        return ResponseEntity.ok("User registered successfully");
    }

    /**
     * 카카오 로그인 콜백 처리 엔드포인트
     * 
     * @param body - 클라이언트에서 전달받은 카카오 인증 코드
     * @return JWT 토큰이 담긴 ResponseEntity
     * 
     *         카카오 로그인 처리 후 사용자의 정보를 받아와, 로그인 처리를 하고 JWT 토큰을 발급합니다.
     */
    @PostMapping("/kakao/callback")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        try {
            // 카카오 API를 사용해 액세스 토큰을 받아옴
            String accessToken = getKakaoAccessToken(code);
            // 액세스 토큰을 사용해 사용자 정보를 받아옴
            Map<String, Object> userInfo = getKakaoUserInfo(accessToken);
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
                // 사용자 이메일을 통해 로그인 처리
                LoginResponse response = userService.kakaoLogin((String) kakaoAccount.get("email"));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email not found in Kakao account");
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("JSON 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 카카오 API로부터 액세스 토큰을 가져오는 메서드
     * 
     * @param code - 카카오 인증 코드
     * @return 액세스 토큰
     * @throws JsonProcessingException - JSON 파싱 중 오류 발생 시 예외 처리
     */
    private String getKakaoAccessToken(String code) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", KAKAO_REST_API_KEY);
        params.add("redirect_uri", "http://localhost:5173/auth/kakao/callback");
        params.add("code", code);

        // 카카오 인증 서버로 요청을 보내 액세스 토큰을 받아옴
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token", request, String.class);

        // 응답에서 액세스 토큰을 추출하여 반환
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response.getBody()).get("access_token").asText();
    }

    /**
     * 카카오 API로부터 사용자 정보를 가져오는 메서드
     * 
     * @param accessToken - 카카오에서 발급받은 액세스 토큰
     * @return 사용자 정보가 담긴 Map
     * @throws JsonProcessingException - JSON 파싱 중 오류 발생 시 예외 처리
     */
    private Map<String, Object> getKakaoUserInfo(String accessToken) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // 액세스 토큰을 헤더에 추가
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 카카오 사용자 정보 API를 호출하여 사용자 정보를 받아옴
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, entity, String.class);

        // 응답에서 JSON을 파싱하여 Map으로 반환
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response.getBody(), Map.class);
    }

    /**
     * 카카오 API로부터 받은 사용자 정보에서 이메일을 추출하는 메서드
     * 
     * @param userInfo - 사용자 정보가 담긴 Map
     * @return 사용자의 이메일 또는 null (이메일이 없을 경우)
     */
    public String getUserEmail(Map<String, Object> userInfo) {
        // properties와 kakao_account 정보가 함께 반환됩니다.
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");

        // 이메일이 있는지 확인 후 추출
        if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
            return (String) kakaoAccount.get("email");
        } else {
            return null; // 이메일이 없을 경우 처리
        }
    }

    /**
     * 현재 로그인한 사용자의 정보를 반환하는 엔드포인트
     * 
     * @param username - 현재 인증된 사용자의 이름
     * @return 사용자의 상세 정보가 담긴 ResponseEntity
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal String username) {
        UserDto userDto = userService.getUserDetails(username);
        return ResponseEntity.ok(userDto);
    }

    /**
     * 특정 사용자의 프로필 정보를 반환하는 엔드포인트
     * 
     * @param id - 조회할 사용자의 ID
     * @return 사용자의 프로필 정보가 담긴 ResponseEntity
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestParam(value = "id") Long id) {
        UserDto userDto = userService.getUserProfile(id);
        return ResponseEntity.ok(userDto);
    }

    /**
     * 사용자의 프로필을 업데이트하는 엔드포인트
     * 
     * @param userDto - 업데이트할 사용자의 정보
     * @return 성공 메시지가 담긴 ResponseEntity
     */
    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody UserDto userDto) {
        userService.updateProfile(userDto);
        return ResponseEntity.ok("Update successfully");
    }

    /**
     * 사용자의 비밀번호를 변경하는 엔드포인트
     * 
     * @param userDto - 비밀번호를 변경할 사용자의 정보
     * @return 성공 메시지가 담긴 ResponseEntity
     */
    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody UserDto userDto) {
        userService.passwordChange(userDto);
        return ResponseEntity.ok("Update successfully");
    }

    /**
     * 회원 탈퇴 엔드포인트
     * 
     * @param request - HTTP 요청 (Authorization 헤더에 JWT 토큰이 있어야 함)
     * @return 성공 메시지가 담긴 ResponseEntity
     * 
     *         현재 로그인한 사용자의 계정을 삭제합니다. JWT 토큰을 통해 사용자를 인증한 후, 해당 사용자를 데이터베이스에서
     *         삭제합니다.
     *         JWT 토큰이 없거나 유효하지 않으면 401 Unauthorized 응답을 반환합니다.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(HttpServletRequest request) {
        // Authorization 헤더에서 JWT 토큰 추출
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        // JWT 토큰에서 사용자 정보를 추출하고, 해당 사용자를 삭제
        String token = authorizationHeader.substring(7);
        userService.deleteUser(token);
        return ResponseEntity.ok("User deleted successfully");
    }
}
