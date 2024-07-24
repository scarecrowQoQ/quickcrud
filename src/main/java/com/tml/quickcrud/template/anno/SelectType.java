package com.tml.quickcrud.template.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SelectType {
    SelectMode selectMode() default SelectMode.eq;

    enum SelectMode{
        like,
        eq,
        ge,
        le
    }
}
