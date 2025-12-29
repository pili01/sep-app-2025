package com.example.WebShopSEP.service;

import com.example.WebShopSEP.dto.user.RegisterDTO;
import com.example.WebShopSEP.model.User;
import com.example.WebShopSEP.model.UserRole;
import com.example.WebShopSEP.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    public UserService(UserRepository userRepository, CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    public User registerUser(RegisterDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setPassword(cryptoService.hashWithSalt(request.getPassword()));
        user.setRole(UserRole.CUSTOMER);

        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> cryptoService.verifyHash(rawPassword, user.getPassword()));
    }
}