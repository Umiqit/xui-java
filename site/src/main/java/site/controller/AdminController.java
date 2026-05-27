package site.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import site.entity.Key;
import site.entity.Payment;
import site.entity.User;
import site.repository.KeyRepository;
import site.repository.PaymentRepository;
import site.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Value("${telegram.admin-ids:}")
    private String adminIdsRaw;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Set<Long> getAdminIds() {
        if (adminIdsRaw == null || adminIdsRaw.isBlank()) return Set.of();
        return Arrays.stream(adminIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    private boolean isAdmin(HttpSession session) {
        Long tgId = (Long) session.getAttribute("tgId");
        return tgId != null && getAdminIds().contains(tgId);
    }

    @Value("${admin.panel-path}")
    private String adminPath;

    @GetMapping("${admin.panel-path}")
    public String admin(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        if (!isAdmin(session)) {
            return "redirect:/profile";
        }

        List<User> users = userRepository.findAll();
        List<Key> keys = keyRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("keys", keys);
        model.addAttribute("payments", payments);
        return "admin";
    }

    @GetMapping("/admin/tickets")
    public String adminTickets(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        if (!isAdmin(session)) {
            return "redirect:/profile";
        }
        return "admin-tickets";
    }
}
