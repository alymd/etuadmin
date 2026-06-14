package com.eduadmin.service;

import com.eduadmin.model.Utilisateur;
import com.eduadmin.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve avec le matricule/nom d'utilisateur : " + username));

        if (!user.isActif()) {
            throw new UsernameNotFoundException("Le compte utilisateur est desactive");
        }

        return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

    @Transactional
    public Utilisateur enregisterUtilisateur(Utilisateur user, PasswordEncoder encoder) {
        if (utilisateurRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Le nom d'utilisateur/matricule existe deja");
        }
        if (utilisateurRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("L'e-mail existe deja");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return utilisateurRepository.save(user);
    }

    public List<Utilisateur> getTousUtilisateurs() {
        return utilisateurRepository.findAll();
    }
}
