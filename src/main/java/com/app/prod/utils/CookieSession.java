package com.app.prod.utils;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CookieSession {

    Mode value() default Mode.REQUIRED;
    String note() default "";

    enum Mode {
        REQUIRED,
        NONE,
        OPTIONAL
    }
}
