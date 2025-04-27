package com.yeelovo.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
    
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加支持SSE的消息转换器
        converters.add(new MappingJackson2HttpMessageConverter() {
            @Override
            public List<MediaType> getSupportedMediaTypes() {
                List<MediaType> mediaTypes = new ArrayList<>(super.getSupportedMediaTypes());
                mediaTypes.add(MediaType.TEXT_EVENT_STREAM);
                return mediaTypes;
            }
        });
    }
} 