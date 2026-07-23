//package io.cvvexxx.users.security;
//
//import io.cvvexxx.users.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//
//@RequiredArgsConstructor
//@Service
//public class DefaultUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public SecurityUser loadUserByUsername(String login) throws UsernameNotFoundException {
//        return userRepository.findByUsernameOrEmail(login, login)
//                .map(SecurityUser::new)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with login: " + login));//TODO
//    }
//}
