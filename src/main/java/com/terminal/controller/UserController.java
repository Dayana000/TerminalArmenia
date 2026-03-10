package com.terminal.controller;

import com.terminal.model.User;
import com.terminal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public Object register(@RequestBody User user) {

        if (user.getName() == null || user.getName().isBlank()) {
            return "El nombre es obligatorio";
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return "El correo es obligatorio";
        }

        if (!user.getEmail().contains("@") || !user.getEmail().contains(".")) {
            return "Correo no válido";
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return "La contraseña es obligatoria";
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            return "El rol es obligatorio";
        }

        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {
            return "El correo ya está registrado";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @PostMapping("/login")
    public Object login(@RequestBody User user) {

        Optional<User> foundUser = userRepository.findByEmail(user.getEmail());

        if (foundUser.isPresent()) {
            boolean passwordCorrect =
                    passwordEncoder.matches(user.getPassword(), foundUser.get().getPassword());

            if (passwordCorrect) {
                return foundUser.get();
            }
        }

        return "Credenciales incorrectas";
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}