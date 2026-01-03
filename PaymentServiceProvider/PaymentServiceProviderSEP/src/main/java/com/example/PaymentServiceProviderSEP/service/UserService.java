package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.user.RegisterDTO;
import com.example.PaymentServiceProviderSEP.model.User;
import com.example.PaymentServiceProviderSEP.model.UserRole;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import com.example.PaymentServiceProviderSEP.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    private final CryptoService cryptoService;

    public UserService(UserRepository userRepository,  CryptoService cryptoService) {
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
        user.setRole(UserRole.ADMIN);

        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> cryptoService.verifyHash(rawPassword, user.getPassword()));
    }
}