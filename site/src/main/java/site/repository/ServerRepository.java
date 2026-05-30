package site.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.entity.Server;

import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {
    List<Server> findByActiveTrue();
}
