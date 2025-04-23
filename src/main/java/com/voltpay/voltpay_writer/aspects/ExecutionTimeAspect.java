package com.voltpay.voltpay_writer.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExecutionTimeAspect {

    @Value("${spring.kafka.consumer.max-poll-records}")
    private Integer recordsToPoll;

    @Around("execution(* com.voltpay.voltpay_writer.consumer.WriteConsumer.processBatchOfMessages(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();
        System.out.println("Processing " + recordsToPoll + " records processed in " + (end - start) + " ms");
        return result;
    }
}
