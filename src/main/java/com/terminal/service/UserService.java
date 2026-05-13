package com.terminal.service;

import com.terminal.model.User;
import com.terminal.repository.UserRepository;
import com.terminal.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    // ── REGISTRO ─────────────────────────────────────────────────
    public Map<String, Object> register(User user) {
        // Validaciones
        if (user.getName() == null || user.getName().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (user.getEmail() == null || user.getEmail().isBlank())
            throw new IllegalArgumentException("El correo es obligatorio");
        if (!user.getEmail().contains("@") || !user.getEmail().contains("."))
            throw new IllegalArgumentException("Correo no valido");
        if (user.getPassword() == null || user.getPassword().isBlank())
            throw new IllegalArgumentException("La contrasena es obligatoria");
        if (user.getPassword().length() < 8)
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres");
        if (!user.getPassword().matches(".*[a-zA-Z].*"))
            throw new IllegalArgumentException("La contrasena debe contener al menos una letra");
        if (!user.getPassword().matches(".*[0-9].*"))
            throw new IllegalArgumentException("La contrasena debe contener al menos un numero");


        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            User existingUser = existing.get();
            // Si ya existe pero no verificado, reenviar código
            if (!Boolean.TRUE.equals(existingUser.getVerified())) {
                String code = generateCode();
                existingUser.setVerificationCode(code);
                existingUser.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));
                userRepository.save(existingUser);
                sendVerificationEmail(existingUser.getEmail(), existingUser.getName(), code);
                return Map.of(
                        "message", "Correo ya registrado pero no verificado. Se reenvio el codigo.",
                        "email", existingUser.getEmail(),
                        "requiresVerification", true
                );
            }
            throw new IllegalArgumentException("El correo ya esta registrado");
        }

        String code = generateCode();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER"); // El registro público siempre crea USER
        user.setVerified(false);
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        sendVerificationEmail(user.getEmail(), user.getName(), code);

        return Map.of(
                "message", "Codigo de verificacion enviado a tu correo",
                "email", user.getEmail(),
                "requiresVerification", true
        );
    }

    // ── VERIFICAR CÓDIGO ─────────────────────────────────────────
    public String verify(String email, String code) {
        if (email == null || code == null)
            throw new IllegalArgumentException("Correo y codigo son obligatorios");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(user.getVerified()))
            throw new IllegalArgumentException("El correo ya fue verificado");

        if (!code.equals(user.getVerificationCode()))
            throw new IllegalArgumentException("Codigo incorrecto");

        // Validar expiración real (15 minutos)
        if (user.getVerificationCodeExpiry() == null ||
                LocalDateTime.now().isAfter(user.getVerificationCodeExpiry())) {
            throw new IllegalArgumentException("El codigo ha expirado. Registrate de nuevo para recibir un codigo nuevo.");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        return "Correo verificado correctamente. Ya puedes iniciar sesion.";
    }

    // ── LOGIN ─────────────────────────────────────────────────────
    public Map<String, Object> login(String email, String password) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("El correo es obligatorio");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("La contrasena es obligatoria");

        Optional<User> foundUser = userRepository.findByEmail(email);
        if (foundUser.isPresent()) {
            User u = foundUser.get();
            if (!Boolean.TRUE.equals(u.getVerified()))
                throw new SecurityException("Debes verificar tu correo antes de iniciar sesion");
            if (passwordEncoder.matches(password, u.getPassword())) {
                String token = jwtUtil.generateToken(u.getEmail(), u.getRole());
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("token", token);
                response.put("id", u.getId());
                response.put("name", u.getName());
                response.put("email", u.getEmail());
                response.put("role", u.getRole());
                return response;
            }
        }
        throw new SecurityException("Credenciales incorrectas");
    }

    // ── CREAR USUARIO PRIVILEGIADO (solo ADMIN) ──────────────────
    public Map<String, Object> createPrivilegedUser(User user) {
        if (user.getName() == null || user.getName().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (user.getEmail() == null || user.getEmail().isBlank())
            throw new IllegalArgumentException("El correo es obligatorio");
        if (!user.getEmail().contains("@") || !user.getEmail().contains("."))
            throw new IllegalArgumentException("Correo no valido");
        if (user.getPassword() == null || user.getPassword().isBlank())
            throw new IllegalArgumentException("La contrasena es obligatoria");
        if (user.getPassword().length() < 8)
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres");
        if (!"ADMIN".equals(user.getRole()) && !"EMPRESA".equals(user.getRole()))
            throw new IllegalArgumentException("Rol no permitido. Use ADMIN o EMPRESA");

        if (userRepository.findByEmail(user.getEmail()).isPresent())
            throw new IllegalArgumentException("El correo ya esta registrado");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setVerified(true);  // Ya verificado, lo crea el ADMIN
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        log.info("[UserService] Usuario {} creado con rol {} por un ADMIN", user.getEmail(), user.getRole());
        return Map.of(
                "message", "Usuario " + user.getRole() + " creado correctamente",
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }

    // ── LISTAR USUARIOS (sin contraseñas) ────────────────────────
    public List<User> getAllUsers() {
        List<User> result = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            result.add(sanitize(u));
        }
        return result;
    }

    // ── PRIVADOS ──────────────────────────────────────────────────
    private String generateCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void sendVerificationEmail(String email, String name, String code) {
        try {
            emailService.sendVerificationEmail(email, name, code);
        } catch (Exception e) {
            log.error("[UserService] Error enviando correo de verificacion a {}: {}", email, e.getMessage(), e);
        }
    }

    private User sanitize(User user) {
        User safe = new User();
        safe.setId(user.getId());
        safe.setName(user.getName());
        safe.setEmail(user.getEmail());
        safe.setRole(user.getRole());
        safe.setVerified(user.getVerified());
        safe.setPassword(null);
        return safe;
    }
}
