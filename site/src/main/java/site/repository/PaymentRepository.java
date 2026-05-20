package site.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.entity.Payment;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
}
