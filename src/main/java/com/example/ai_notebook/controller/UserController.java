package com.example.ai_notebook.controller;

import com.example.ai_notebook.entity.UserEntity;
import com.example.ai_notebook.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
//@CrossOrigin(origins = "http://localhost:5173") // React 프론트 허용
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public UserEntity signup(@RequestBody Map<String, String> req) {
        return userService.register(req.get("username"), req.get("password"), req.get("email"));
    }

    @PostMapping("/login")
    public Object login(@RequestBody Map<String, String> req) {
        Optional<UserEntity> user = userService.login(req.get("username"), req.get("password"));
        return user.orElse(null);
    }
}
