package com.spring.bean.annotation;

import java.lang.annotation.*;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:19
 * @return
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value() default  "";
}
