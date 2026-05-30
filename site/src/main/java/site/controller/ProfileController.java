package site.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import site.entity.Key;
import site.entity.Server;
import site.entity.User;
import site.repository.KeyRepository;
import site.repository.PaymentRepository;
import site.repository.ServerRepository;
import site.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ServerRepository serverRepository;

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
        List<Server> servers = serverRepository.findAll();
        Map<Long, String> serverNames = new HashMap<>();
        for (Server s : servers) {
            serverNames.put(s.getId(), s.displayName());
        }

        model.addAttribute("user", user);
        model.addAttribute("keys", keys);
        model.addAttribute("serverNames", serverNames);
        return "profile";
    }
}
