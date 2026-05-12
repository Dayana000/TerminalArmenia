package com.terminal.controller;

import com.terminal.model.User;
import com.terminal.repository.UserRepository;
import com.terminal.security.JwtUtil;
import com.terminal.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/users")
@CrossOrigin("*")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

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
        if (existingUser.isPresent()) {
            User existing = existingUser.get();
            if (!Boolean.TRUE.equals(existing.getVerified())) {
                String code = generateCode();
                existing.setVerificationCode(code);
                userRepository.save(existing);
                sendVerificationEmail(existing.getEmail(), existing.getName(), code);
                return ResponseEntity.ok(Map.of(
                        "message", "Correo ya registrado pero no verificado. Se reenvio el codigo.",
                        "email", existing.getEmail(),
                        "requiresVerification", true
                ));
            }
            return ResponseEntity.badRequest().body("El correo ya esta registrado");
        }

        String code = generateCode();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setVerified(false);
        user.setVerificationCode(code);
        userRepository.save(user);

        // DEBUG temporal para verificar variables de entorno
        System.out.println(">>> MAIL_USERNAME ENV: " + System.getenv("MAIL_USERNAME"));
        System.out.println(">>> MAIL_PASSWORD ENV: " + (System.getenv("MAIL_PASSWORD") != null ? "****SET****" : "NULL - NO CONFIGURADO"));
        System.out.println(">>> SPRING MAIL USER PROP: " + System.getProperty("spring.mail.username"));

        sendVerificationEmail(user.getEmail(), user.getName(), code);

        return ResponseEntity.ok(Map.of(
                "message", "Codigo de verificacion enviado a tu correo",
                "email", user.getEmail(),
                "requiresVerification", true
        ));
    }

    @PostMapping("/verify")
    public Object verify(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code  = body.get("code");

        if (email == null || code == null)
            return ResponseEntity.badRequest().body("Correo y codigo son obligatorios");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return ResponseEntity.badRequest().body("Usuario no encontrado");

        User user = userOpt.get();

        if (Boolean.TRUE.equals(user.getVerified()))
            return ResponseEntity.badRequest().body("El correo ya fue verificado");

        if (!code.equals(user.getVerificationCode()))
            return ResponseEntity.badRequest().body("Codigo incorrecto");

        user.setVerified(true);
        user.setVerificationCode(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Correo verificado correctamente. Ya puedes iniciar sesion."));
    }

    @PostMapping("/login")
    public Object login(@RequestBody User user) {

        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body("El correo es obligatorio");
        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body("La contrasena es obligatoria");

        Optional<User> foundUser = userRepository.findByEmail(user.getEmail());

        if (foundUser.isPresent()) {
            User u = foundUser.get();

            if (!Boolean.TRUE.equals(u.getVerified()))
                return ResponseEntity.status(403).body("Debes verificar tu correo antes de iniciar sesion");

            boolean passwordCorrect = passwordEncoder.matches(user.getPassword(), u.getPassword());
            if (passwordCorrect) {
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
        for (User u : users) safeUsers.add(hidePassword(u));
        return safeUsers;
    }

    private String generateCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void sendVerificationEmail(String email, String name, String code) {
        try {
            emailService.sendVerificationEmail(email, name, code);
        } catch (Exception e) {
            System.err.println("[UserController] Error enviando correo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private User hidePassword(User user) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setName(user.getName());
        safeUser.setEmail(user.getEmail());
        safeUser.setRole(user.getRole());
        safeUser.setVerified(user.getVerified());
        safeUser.setPassword(null);
        return safeUser;
    }
}