package dev.nilswitt.mission_manager.security;

import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.UserService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LocalUserDetailsManager implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(LocalUserDetailsManager.class);
    private final UserService userService;

    public LocalUserDetailsManager(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        Optional<User> optionalUser = userService.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException(String.format("No user found with username '%s'.", username));
        }

        return optionalUser.get();
    }
}
