package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.user.LoginDTO;
import com.example.WebShopSEP.dto.user.LoginResponseDTO;
import com.example.WebShopSEP.dto.user.RegisterDTO;
import com.example.WebShopSEP.jwt.JwtService;
import com.example.WebShopSEP.model.User;
import com.example.WebShopSEP.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO request) {
        try {
            User savedUser = userService.registerUser(request);
            return ResponseEntity.ok("User registered with ID: " + savedUser.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while registering the user");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO request, HttpServletRequest httpRequest) {
        User user = userService.authenticate(request.getEmail(), request.getPassword())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(token, user.getId(), user.getEmail(), user.getRole().toString()));
    }
}