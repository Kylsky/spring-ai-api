package com.yeelovo.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求超时时间为5分钟
        configurer.setDefaultTimeout(300000);
        
        // 使用自定义线程池处理异步请求
        // configurer.setTaskExecutor(taskExecutor());
        
        // 注册异步拦截器
        // configurer.registerCallableInterceptors(interceptor);
    }
    
    // 自定义线程池配置可以在这里添加
    // @Bean
    // public ThreadPoolTaskExecutor taskExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     executor.setCorePoolSize(10);
    //     executor.setMaxPoolSize(50);
    //     executor.setQueueCapacity(100);
    //     executor.setKeepAliveSeconds(60);
    //     executor.setThreadNamePrefix("async-executor-");
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //     return executor;
    // }
} 