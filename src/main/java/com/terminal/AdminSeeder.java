package com.terminal;

import com.terminal.model.User;
import com.terminal.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Se ejecuta una sola vez al arrancar la aplicacion.
 * Si no existe ningun ADMIN en la base de datos, crea el admin inicial
 * con los datos configurados en las variables de entorno.
 */
@Slf4j
@Component
public class AdminSeeder implements ApplicationRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.seed.name:Dayana Andrea Buitrago}")
    private String adminName;

    @Value("${admin.seed.email:buitragodayana664@gmail.com}")
    private String adminEmail;

    @Value("${admin.seed.password:Dayana042818}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        var existingUser = userRepository.findByEmail(adminEmail);

        if (existingUser.isEmpty()) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setVerified(true);
            admin.setVerificationCode(null);
            userRepository.save(admin);
            log.info("[AdminSeeder] Admin inicial creado: {}", adminEmail);
        } else {
            User admin = existingUser.get();
            admin.setRole("ADMIN");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setVerified(true);
            userRepository.save(admin);
            log.info("[AdminSeeder] Admin existente actualizado con nuevas credenciales: {}", adminEmail);
        }
    }
}
