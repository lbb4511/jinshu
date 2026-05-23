package com.jinshu.common.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    String operation();

    String targetType() default "";

    String targetName() default "";
}
