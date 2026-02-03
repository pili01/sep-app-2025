package com.example.Bank.controller;

import com.example.Bank.dto.user.LoginDTO;
import com.example.Bank.dto.user.LoginResponseDTO;
import com.example.Bank.dto.user.RegisterDTO;
import com.example.Bank.jwt.JwtService;
import com.example.Bank.model.User;
import com.example.Bank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO request) {
        try {
            User savedUser = userService.registerUser(request);
            log.info("User registered successfully");
            securityLog.info("User registered successfully");
            return ResponseEntity.ok("User registered with ID: " + savedUser.getId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid username or password");
            securityLog.warn("Invalid username or password");
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Unknown registration error");
            securityLog.error("Unknown registration error");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while registering the user");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO request, HttpServletRequest httpRequest) {
        securityLog.info("Pokusaj prijavljivanja korisinka");
        log.info("Pokusaj prijavljivanja korisinka");

        User user = userService.authenticate(request.getEmail(), request.getPassword())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        String token = jwtService.generateToken(user);

        securityLog.info("Uspesno prijavljen korisnik");
        log.info("Uspesno prijavljen korisnik");

        return ResponseEntity.ok(new LoginResponseDTO(token, user.getId(), user.getEmail(), user.getRole().toString()));
    }
}