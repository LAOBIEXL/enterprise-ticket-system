package com.example.demo.aspect;

import com.example.demo.dto.LoginRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerLogAspectTests {

    private final ControllerLogAspect aspect = new ControllerLogAspect();

    @Test
    void shouldExecuteControllerMethodAndReturnItsResult() throws Throwable {
        ProceedingJoinPoint joinPoint = createJoinPoint();
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.logControllerExecution(joinPoint);

        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldRethrowControllerException() throws Throwable {
        ProceedingJoinPoint joinPoint = createJoinPoint();
        RuntimeException exception = new RuntimeException("test error");
        when(joinPoint.proceed()).thenThrow(exception);

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> aspect.logControllerExecution(joinPoint)
        );

        assertEquals(exception, actual);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldRedactLoginRequestArguments() {
        String arguments = aspect.formatArguments(
                new Object[]{new LoginRequest("admin", "secret-password")}
        );

        assertEquals("[LoginRequest[REDACTED]]", arguments);
    }

    private ProceedingJoinPoint createJoinPoint() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserController.test()");
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        return joinPoint;
    }
}
