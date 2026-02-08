package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.user.LoginDTO;
import com.example.PaymentServiceProviderSEP.dto.user.LoginResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.user.RegisterDTO;
import com.example.PaymentServiceProviderSEP.jwt.JwtService;
import com.example.PaymentServiceProviderSEP.model.User;
import com.example.PaymentServiceProviderSEP.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    // otkomentarisati ako treba dodati novog admina, bolje da bude onemogucena registracija super admina
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
    public ResponseEntity<?> login(@RequestBody LoginDTO request) {
        var userOptional = userService.authenticate(request.getEmail(), request.getPassword());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        User user = userOptional.get();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole().toString()
        ));
    }
}