package site.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.entity.Key;

import java.util.List;

public interface KeyRepository extends JpaRepository<Key, Long> {
    List<Key> findByUserId(Long userId);
}
