package com.academicpassport.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String idStr) throws UsernameNotFoundException {
        try {
            Long id = Long.parseLong(idStr);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + idStr));
            return new UserPrincipal(user);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("User id is not a number: " + idStr);
        }
    }
}
