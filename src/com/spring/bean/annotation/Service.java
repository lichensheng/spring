package com.spring.bean.annotation;

import java.lang.annotation.*;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:22
 * @return
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String value() default "";
}
