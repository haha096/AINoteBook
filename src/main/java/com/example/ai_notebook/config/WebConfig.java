package com.example.ai_notebook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 기존 uploads 폴더 (절대경로 확인 필요!)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");

        // 2. ⭐️ 이게 핵심! static 폴더의 모든 파일을 연결해줍니다.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // 새로고침하면 바로 반영되게!
    }
}
