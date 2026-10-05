package ru.example.taskmanager.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceTimingAspect {
  private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

  @Around("execution(public * ru.example.taskmanager.service.TaskService.*(..))")
  public Object measure(ProceedingJoinPoint call) throws Throwable {
    long start = System.nanoTime();
    try {
      return call.proceed();
    } finally {
      log.debug(
          "{}: {} ms", call.getSignature().getName(), (System.nanoTime() - start) / 1_000_000);
    }
  }
}
