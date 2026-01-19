package com.example.langchain4j.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许的源（前端地址），* 表示允许所有源（开发环境可用，生产环境建议指定具体域名）
        config.addAllowedOrigin("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 允许的请求方法（GET、POST、PUT等）
        config.addAllowedMethod("*");
        // 允许携带Cookie（如果需要）
        config.setAllowCredentials(true);
        // 预检请求的缓存时间（秒），减少OPTIONS请求次数
        config.setMaxAge(3600L);

        // 2. 配置拦截路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效
        source.registerCorsConfiguration("/**", config);

        // 3. 返回CORS过滤器
        return new CorsFilter(source);
    }
}
