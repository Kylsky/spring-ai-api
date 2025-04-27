package com.yeelovo.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池统一配置类
 * 为不同业务场景提供专用线程池，优化资源利用
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    // CPU核心数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    
    /**
     * 流式处理专用线程池
     * 适合I/O密集型任务，如SSE流式响应
     * 特点：线程数量相对更多，避免阻塞
     */
    @Bean("streamTaskExecutor")
    public Executor streamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数 = CPU核心数 * 2
        // I/O密集型任务适合更多的线程数
        executor.setCorePoolSize(CPU_COUNT * 2);
        
        // 最大线程数 = CPU核心数 * 10
        // 允许更高的并发，适应突发请求
        executor.setMaxPoolSize(CPU_COUNT * 10);
        
        // 队列容量
        // 较大的队列容量可以缓冲更多的任务
        executor.setQueueCapacity(200);
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 线程名前缀，便于监控和调试
        executor.setThreadNamePrefix("stream-task-");
        
        // 拒绝策略：调用者线程执行
        // 高负载时，由调用线程自己执行任务，不会丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 关闭线程池等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        // 允许核心线程超时
        // 长时间空闲的核心线程也会被回收，进一步节约资源
        executor.setAllowCoreThreadTimeOut(true);
        
        return executor;
    }
    
    /**
     * 非流式处理专用线程池
     * 适合计算密集型任务，如阻塞式API调用
     * 特点：线程数与CPU核心数相近，减少上下文切换
     */
    @Bean("blockingTaskExecutor")
    public Executor blockingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数 = CPU核心数 + 1
        // 计算密集型任务适合接近CPU核心数的线程数
        executor.setCorePoolSize(CPU_COUNT + 1);
        
        // 最大线程数 = CPU核心数 * 2
        // 最大并发有限制，减少资源争用
        executor.setMaxPoolSize(CPU_COUNT * 2);
        
        // 队列容量
        // 适中的队列容量，平衡吞吐量和响应时间
        executor.setQueueCapacity(100);
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 线程名前缀，便于监控和调试
        executor.setThreadNamePrefix("blocking-task-");
        
        // 拒绝策略：调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 关闭线程池等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        return executor;
    }
    
    /**
     * 专用于负载测试的线程池
     * 特点：更大的容量和更多的线程，适合高并发测试场景
     */
    @Bean("loadTestExecutor")
    public Executor loadTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 支持高并发的核心线程数
        executor.setCorePoolSize(CPU_COUNT * 4);
        
        // 非常高的最大线程数，用于测试极限场景
        executor.setMaxPoolSize(CPU_COUNT * 20);
        
        // 大队列，可以排队更多的测试任务
        executor.setQueueCapacity(500);
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(120);
        
        // 线程名前缀
        executor.setThreadNamePrefix("load-test-");
        
        // 拒绝策略：调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 关闭线程池等待时间（秒）
        executor.setAwaitTerminationSeconds(120);
        
        return executor;
    }
} 