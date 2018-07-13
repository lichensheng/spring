package com.spring.bean.annotation;

import java.lang.annotation.*;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:15
 * @return
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default  "";
}
