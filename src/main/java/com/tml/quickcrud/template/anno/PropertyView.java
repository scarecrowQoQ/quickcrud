package com.tml.quickcrud.template.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 前端表单，表格字段展示注解
 * CNName： 表示该字段的中文名，将会作为字段描述显示
 * type：表示该字段的类型，
 *  string：字符串类型
 *  switch：开关类型，1,0表示开关
 *  data：  日期类型
 *  int：   数字类型
 *  select：下拉框类型
 *  object: 引用类型
 * reference：表示该字段的引用类型，
 * groupId: 字段分组，用于表格当字段过多导致表格过长时字段合并显示
 * groupName: 字段分组，用于表单输入时分组展示
 * inputTips: 字段输入提示,描述约定的输入格式
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PropertyView {
    String CNName();

    PropertyType type() default PropertyType.String;

    Class<?> reference() default Void.class;

    String referenceName() default "";

    int groupId() default 0;

    String groupName() default "";

    // 字段输入提示
    String inputTips() default "";

    // 是否在查询条件中展示
    boolean selectParamShow() default true;

    // 是否在表格中展示
    boolean tableShow() default true;

    // 是否在添加表单中展示
    boolean insertFormShow() default true;

    // 是否在编辑表单中展示
    boolean updateFormShow() default true;

    // 跳过解析
    boolean skipParsing() default false;

    enum PropertyType{
        String,
        Map,
        Number,
        Switch,
        Date,
        Time,
        Select,
        Object,
        Json,
        Img,
        FormSlot,
        ;
    }
}
