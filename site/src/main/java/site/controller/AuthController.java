package site.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import site.dto.TelegramAuthData;
import site.entity.User;
import site.service.TelegramAuthService;
import site.service.UserService;

import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private TelegramAuthService authService;

    @Autowired
    private UserService userService;

    @Value("${telegram.bot-username}")
    private String botUsername;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("botUsername", botUsername);
        return "login";
    }

    @PostMapping("/auth/telegram")
    @ResponseBody
    public ResponseEntity<?> telegramAuth(@RequestBody TelegramAuthData data, HttpSession session) {
        if (!authService.verify(data)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid Telegram auth data"));
        }

        User user = userService.findOrCreate(data.getId(), data.getUsername(), data.getFirstName());
        session.setAttribute("userId", user.getId());
        session.setAttribute("tgId", user.getTgId());
        session.setAttribute("username", user.getUsername());

        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
