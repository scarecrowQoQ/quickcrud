package com.tml.quickcrud.template.domain;

import java.lang.reflect.Method;
import java.util.Objects;


public interface BaseEnum {

    /**
     * 获取枚举标识
     *
     * @return
     */
    <T extends Object> T getCode();

    /**
     * 获取枚举描述
     *
     * @return
     */
    <T extends Object> T getDesc();

    /**
     * 通过枚举类型和code值获取对应的枚举类型
     *
     * @param enumType
     * @param code
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> T valueOf(Class<? extends BaseEnum> enumType, Integer code) {
        if (enumType == null || code == null) {
            return null;
        }
        T[] enumConstants = (T[]) enumType.getEnumConstants();
        if (enumConstants == null) {
            return null;
        }
        for (T enumConstant : enumConstants) {
            int enumCode = enumConstant.getCode();
            if (code.equals(enumCode)) {
                return enumConstant;
            }
        }
        return null;
    }

    /**
     * 获取枚举类
     *
     * @param clazz
     * @param code
     * @param <T>
     * @return
     */
    static <T extends Enum<T>> T getEnumByCode(Class<T> clazz, Object code) {
        if (Objects.isNull(code)) {
            return null;
        }
        T result = null;
        try {
            T[] arr = clazz.getEnumConstants();
            Method targetMethod = clazz.getDeclaredMethod("getCode");
            Object typeNameVal;
            for (T entity : arr) {
                typeNameVal = targetMethod.invoke(entity);
                String codeStr = String.valueOf(code);
                if (codeStr.equalsIgnoreCase(String.valueOf(typeNameVal))) {
                    result = entity;
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }

}