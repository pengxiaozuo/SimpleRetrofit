package com.peng.retrofit.http;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {
    String value() default "";

    boolean encoded() default false;
}
