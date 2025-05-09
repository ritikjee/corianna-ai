package com.corianna.auth_service.services;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.corianna.auth_service.entity.Device;
import com.corianna.auth_service.entity.User;
import com.corianna.auth_service.repository.DeviceRepository;
import com.corianna.auth_service.repository.UserRepository;
import com.corianna.auth_service.utils.GenerateKeys;
import com.corianna.auth_service.utils.JwtConfig;
import com.corianna.auth_service.utils.MessageProducer;

import ua_parser.Client;
import ua_parser.Parser;

@Service
public class AuthService implements UserDetailsService {

    @Value("${jwt.secret.otp_secret}")
    private String otpSecret;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationManager authenticationManager;
    private final DeviceRepository deviceRepository;
    private final Parser parser;
    private final MessageProducer messageProducer;
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(
            UserRepository userRepository,
            @Lazy BCryptPasswordEncoder bCryptPasswordEncoder,
            AuthenticationManager authenticationManager,
            DeviceRepository deviceRepository,
            Parser parser,
            MessageProducer messageProducer,
            JwtConfig jwtConfig,
            RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.authenticationManager = authenticationManager;
        this.deviceRepository = deviceRepository;
        this.parser = parser;
        this.messageProducer = messageProducer;
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Cacheable(value = "user-details", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndVerified(username, true).orElse(null);

        UserBuilder userBuilder = org.springframework.security.core.userdetails.User.withUsername(username);
        userBuilder.password(user.getPassword());

        return userBuilder.build();
    }

    @Cacheable(value = "users", key = "#email")
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Cacheable(value = "users", key = "#email")
    public User getVerifiedUser(String email) {
        return userRepository.findByEmailAndVerified(email, true).orElse(null);
    }

    public String register(User user) {

        User exists = getUserByEmail(user.getEmail());

        if (exists != null) {
            throw new RuntimeException("User already exists with this email");
        }

        String OTP = GenerateKeys.generateOtp();

        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setVerified(false);

        Map<String, Object> message = Map.of("email", user.getEmail(), "otp", OTP);
        messageProducer.sendMessage(message);

        redisTemplate.opsForValue().set("otp::" + user.getEmail(), OTP, Duration.ofMinutes(15));

        Map<String, Object> otpState = Map.of("email", user.getEmail());

        String state = jwtConfig.encodeToken(otpSecret, 15 * 60 * 1000, otpState);

        return state;
    }

    @CacheEvict(value = "users", key = "#email")
    public Device login(String email, String password, String ipAddress, String userAgent) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        User user = getVerifiedUser(email);

        if (user == null) {
            throw new RuntimeException("User does not exists");
        }

        Client c = parser.parse(userAgent);
        Device newDevice = new Device();
        newDevice.setIpAddress(ipAddress);
        newDevice.setDeviceType(c.device.family);
        newDevice.setOs(c.os.family);
        newDevice.setDeviceAgent(c.userAgent.family);
        newDevice.setUser(user);

        Map<String, Object> deviceState = Map.of(
                "email", email,
                "ipAddress", ipAddress,
                "deviceAgent", c.userAgent.family,
                "deviceType", c.device.family,
                "os", c.os.family);

        messageProducer.sendMessage(deviceState);

        return deviceRepository.save(newDevice);

    }

    public String forgotPassword(String email) {
        User user = getUserByEmail(email);

        if (user == null) {
            throw new RuntimeException("User does not exists");
        }

        String OTP = GenerateKeys.generateOtp();

        Map<String, Object> message = Map.of("email", email, "otp", OTP);
        messageProducer.sendMessage(message);

        redisTemplate.opsForValue().set("otp::" + email, OTP, Duration.ofMinutes(15));

        Map<String, Object> otpState = Map.of("email", email);

        String state = jwtConfig.encodeToken(otpSecret, 15 * 60 * 1000, otpState);

        return state;
    }

    public String resetPassword(String state, String password) {
        Map<String, Object> decodedState = jwtConfig.decodeToken(otpSecret, state);

        String email = (String) decodedState.get("email");

        if (email == null) {
            throw new RuntimeException("Invalid OTP state");
        }

        String redisOtp = redisTemplate.opsForValue().get("otp::" + email);

        if (redisOtp == null) {
            throw new RuntimeException("OTP expired");
        }

        User user = getUserByEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(user);

        redisTemplate.delete("otp::" + email);
        redisTemplate.delete("users::" + email);

        return "Password reset successfully";
    }

    public String verifyOtp(String state, String otp) {
        Map<String, Object> decodedState = jwtConfig.decodeToken(otpSecret, state);

        String email = (String) decodedState.get("email");

        if (email == null) {
            throw new RuntimeException("Invalid OTP state");
        }

        String redisOtp = redisTemplate.opsForValue().get("otp::" + email);

        if (redisOtp == null) {
            throw new RuntimeException("OTP expired");
        }

        if (!redisOtp.equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = getUserByEmail(email);
        user.setVerified(true);
        userRepository.save(user);

        redisTemplate.delete("otp::" + email);
        redisTemplate.delete("users::" + email);

        return "OTP verified successfully";
    }

    public String resendOTP(String state) {
        Map<String, Object> decodedState = jwtConfig.decodeToken(otpSecret, state);

        String email = (String) decodedState.get("email");

        if (email == null) {
            throw new RuntimeException("Invalid OTP state");
        }

        String OTP = GenerateKeys.generateOtp();

        Map<String, Object> message = Map.of("email", email, "otp", OTP);
        messageProducer.sendMessage(message);

        redisTemplate.opsForValue().set("otp::" + email, OTP, Duration.ofMinutes(15));

        return "OTP resent successfully";
    }

}