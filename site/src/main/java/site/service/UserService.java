package site.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.entity.User;
import site.repository.UserRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> findByTgId(long tgId) {
        return userRepository.findByTgId(tgId);
    }

    public User findOrCreate(long tgId, String username, String fullName) {
        return userRepository.findByTgId(tgId).orElseGet(() -> {
            User user = new User();
            user.setTgId(tgId);
            user.setUsername(username);
            user.setFullName(fullName);
            user.setBalance(0.0);
            user.setCreatedAt(Timestamp.from(Instant.now()));
            return userRepository.save(user);
        });
    }
}
