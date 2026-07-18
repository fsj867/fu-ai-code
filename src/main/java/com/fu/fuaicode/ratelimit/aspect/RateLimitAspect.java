package com.fu.fuaicode.ratelimit.aspect;

import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.model.entity.User;
import com.fu.fuaicode.ratelimit.annonation.RateLimit;
import com.fu.fuaicode.ratelimit.enums.RateLimitType;
import com.fu.fuaicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.*;
import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;
   
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        
        // 获得key
        String key = generateRateLimitKey(point, rateLimit);
        // 获得限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 初始化限流器
        initRateLimiter(rateLimiter, key, rateLimit);
        // 尝试获得令牌,tryAcquire(1)表示尝试获得1个令牌,如果成功,返回true,否则返回false,成功执行后续业务
        if (!rateLimiter.tryAcquire(1)){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
    
    }
    // 初始化限流器
    private void initRateLimiter(RRateLimiter rateLimiter, String key, RateLimit rateLimit) {
        //  如果已经有限流器,刷新时间
        if (rateLimiter.isExists()){
                //  如果已经有限流器,刷新时间
                rateLimiter.expire(Duration.ofSeconds(rateLimit.rateInterval() * 2L));
                return;
        }
        // 分布式锁
        String lockKey = "rate_limit_lock:" + key;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLock = lock.tryLock(1, 2, TimeUnit.SECONDS); // 等待1秒，持有2秒，尝试获取锁，返回是否成功
            if (!isLock) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"限流组件启动失败");
            }
            // 双重检查,防止并发问题，确保只初始化一次
            if (!rateLimiter.isExists()) {

                boolean success = rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), RateIntervalUnit.SECONDS); // 尝试设置限流器,返回是否成功
                if (success) {
                    // 如果成功设置限流器，设置过期时间
                    rateLimiter.expire(Duration.ofSeconds(rateLimit.rateInterval() * 2L)); // 设置过期时间为2倍限流时间
                }else {
                    log.warn("Failed to set rate limiter for key: {}", key); // 记录警告日志
                }
            }
        } catch (InterruptedException e) {
            // 捕获异常并中断当前线程
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "限流组件初始化失败");
        }finally {
            if (lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
    private String generateRateLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        // 添加自定义前缀
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        // 根据限流类型生成不同的key
        switch (rateLimit.limitType()) {
            case API:
                // 接口级别：方法名
                MethodSignature signature = (MethodSignature) point.getSignature(); // 获取方法签名,包含方法名和参数类型
                Method method = signature.getMethod(); // 获取方法对象
                // 构建key
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".").append(method.getName());
                break;
            case USER:
                // 用户级别：用户ID
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();  // 获取请求上下文
                    // 从请求上下文获取登录用户
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        User loginUser = userService.getLoginUser(request); // 获取登录用户
                        keyBuilder.append("user:").append(loginUser.getId()); // 构建key
                    } else {
                        // 无法获取请求上下文，使用IP限流
                        keyBuilder.append("ip:").append(getClientIP());
                    }
                } catch (BusinessException e) {
                    // 未登录用户使用IP限流
                    keyBuilder.append("ip:").append(getClientIP());
                }
                break;
            case IP:
                // IP级别：客户端IP
                keyBuilder.append("ip:").append(getClientIP());
                break;
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return keyBuilder.toString();
    }

    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(); // 获取请求上下文
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For"); // 获取X-Forwarded-For头
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP"); // 获取X-Real-IP头
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr(); // 获取客户端IP
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown"; // 返回客户端IP
    }
 
}
