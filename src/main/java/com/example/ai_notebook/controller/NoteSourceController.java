package com.example.ai_notebook.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class NoteSourceController {

//    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public Map<String,Object> upload(@PathVariable Long id,
//                                     @RequestPart("file") MultipartFile file) throws Exception {
//        Path root = Paths.get("uploads").resolve(String.valueOf(id));
//        Files.createDirectories(root);
//
//        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
//        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
//        String stored = stamp + "_" + original;
//
//        Files.copy(file.getInputStream(), root.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
//
//        Map<String,Object> res = new HashMap<>();
//        res.put("name", original);
//        res.put("path", "/uploads/" + id + "/" + stored);
//        return res;
//    }
}
