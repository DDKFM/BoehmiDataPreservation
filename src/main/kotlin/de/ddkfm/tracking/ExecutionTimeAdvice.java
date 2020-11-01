package de.ddkfm.tracking;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@ConditionalOnExpression("${aspect.enabled:true}")
public class ExecutionTimeAdvice {

    @Around("@annotation(de.ddkfm.tracking.TrackExecutionTime)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object object = point.proceed();
        long endtime = System.currentTimeMillis();
        System.out.println("Class Name: "+ point.getSignature().getDeclaringTypeName() +". Method Name: "+ point.getSignature().getName() + ". Time taken for Execution is : " + (endtime-startTime) +"ms");
        return object;
    }

   /* @Before("execution(* com.mailshine.springboot.aop.aspectj.service.EmployeeService.*(..))")
    public void executionTimeBefore(JoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        //long endtime = System.currentTimeMillis();
        log.info("Method Name: "+ point.getSignature().getName() + ". Time taken for Execution is : " + (startTime) +"ms");
    }

    @After("execution(* com.mailshine.springboot.aop.aspectj.service.EmployeeService.*(..))")
    public void executionTimeAfter(JoinPoint point) throws Throwable {
       // long startTime = System.currentTimeMillis();
        long endtime = System.currentTimeMillis();
        log.info("Method Name: "+ point.getSignature().getName() + ". Time taken for Execution is : " + (endtime) +"ms");
    }*/
}
