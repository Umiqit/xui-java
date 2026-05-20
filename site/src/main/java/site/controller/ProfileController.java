package site.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import site.entity.Key;
import site.entity.User;
import site.repository.KeyRepository;
import site.repository.PaymentRepository;
import site.repository.UserRepository;

import java.util.List;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }

        List<Key> keys = keyRepository.findByUserId(userId);

        model.addAttribute("user", user);
        model.addAttribute("keys", keys);
        return "profile";
    }
}
