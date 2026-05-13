package com.terminal.controller;

import com.terminal.model.User;
import com.terminal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            Map<String, Object> result = userService.register(user);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        try {
            String message = userService.verify(body.get("email"), body.get("code"));
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            Map<String, Object> result = userService.login(user.getEmail(), user.getPassword());
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            int status = e.getMessage().contains("verificar") ? 403 : 401;
            return ResponseEntity.status(status).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Solo accesible por ADMIN — crea cuentas de ADMIN o EMPRESA
    @PostMapping("/admin/create")
    public ResponseEntity<?> createPrivilegedUser(@RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.createPrivilegedUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}