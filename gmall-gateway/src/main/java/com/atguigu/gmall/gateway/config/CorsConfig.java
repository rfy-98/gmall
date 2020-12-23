package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){
        //初始化cors配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //要写具体的域名
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        //能否携带cokkie
        corsConfiguration.setAllowCredentials(true);


        // 添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(corsConfigurationSource);


    }
}
