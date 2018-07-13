package com.spring.bean.annotation;

import java.lang.annotation.*;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:17
 * @return
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}
