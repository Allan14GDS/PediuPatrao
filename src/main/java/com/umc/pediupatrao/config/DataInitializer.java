package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.enums.Role;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        return args -> {
            upsertUsuario(repo, passwordEncoder, "admin", "admin", Role.ADMIN);
            upsertUsuario(repo, passwordEncoder, "gerente", "gerente", Role.GERENTE);
            upsertUsuario(repo, passwordEncoder, "atendente", "atendente", Role.ATENDENTE);
        };
    }

    private void upsertUsuario(UsuarioRepository repo, PasswordEncoder passwordEncoder,
                               String username, String senha, Role role) {
        Usuario usuario = repo.findByUsername(username).orElseGet(Usuario::new);
        boolean novo = usuario.getId() == null;

        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(senha));
        usuario.setRole(role);
        repo.save(usuario);

        System.out.println(novo
                ? "Usuário " + username + " criado!"
                : "Usuário " + username + " atualizado!");
    }
}
