package com.terminal.controller;

import com.terminal.model.User;
import com.terminal.repository.UserRepository;
import com.terminal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/users")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public Object register(@RequestBody User user) {

        if (user.getName() == null || user.getName().isBlank())
            return ResponseEntity.badRequest().body("El nombre es obligatorio");

        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body("El correo es obligatorio");

        if (!user.getEmail().contains("@") || !user.getEmail().contains("."))
            return ResponseEntity.badRequest().body("Correo no valido");

        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body("La contrasena es obligatoria");

        if (user.getPassword().length() < 8)
            return ResponseEntity.badRequest().body("La contrasena debe tener al menos 8 caracteres");

        if (!user.getPassword().matches(".*[a-zA-Z].*"))
            return ResponseEntity.badRequest().body("La contrasena debe contener al menos una letra");

        if (!user.getPassword().matches(".*[0-9].*"))
            return ResponseEntity.badRequest().body("La contrasena debe contener al menos un numero");

        if (user.getRole() == null || user.getRole().isBlank())
            return ResponseEntity.badRequest().body("El rol es obligatorio");

        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent())
            return ResponseEntity.badRequest().body("El correo ya esta registrado");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(hidePassword(savedUser));
    }

    @PostMapping("/login")
    public Object login(@RequestBody User user) {

        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body("El correo es obligatorio");

        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body("La contrasena es obligatoria");

        Optional<User> foundUser = userRepository.findByEmail(user.getEmail());

        if (foundUser.isPresent()) {
            boolean passwordCorrect =
                    passwordEncoder.matches(user.getPassword(), foundUser.get().getPassword());

            if (passwordCorrect) {
                User u = foundUser.get();
                String token = jwtUtil.generateToken(u.getEmail(), u.getRole());

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("token", token);
                response.put("id", u.getId());
                response.put("name", u.getName());
                response.put("email", u.getEmail());
                response.put("role", u.getRole());

                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }

    @GetMapping
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<User> safeUsers = new ArrayList<>();
        for (User u : users) {
            safeUsers.add(hidePassword(u));
        }
        return safeUsers;
    }

    private User hidePassword(User user) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setName(user.getName());
        safeUser.setEmail(user.getEmail());
        safeUser.setRole(user.getRole());
        safeUser.setPassword(null);
        return safeUser;
    }
}