package com.tml.quickcrud.template.excp;

public interface BaseExceptionTemplate {
    // 入参属性值非法异常
    default void parameterIllegal(String desc) {
        throw new CustomException(ResultCode._PARAMETER_ILLEGAL,desc == null ? ResultCode._PARAMETER_ILLEGAL.getMessage() : desc);
    }

    // 查询结果为空
    default void recordNotExist(String desc) {
        throw new CustomException(ResultCode._RECORD_NOT_EXIST,desc == null ? ResultCode._RECORD_NOT_EXIST.getMessage() : desc);
    }

    // 插入结果失败异常
    default void insertError(String desc) {
        throw new CustomException(ResultCode._INSERT_ERROR,desc == null ? ResultCode._INSERT_ERROR.getMessage() : desc);
    }

    // 重复插入结果失败
    default void insertRepeatError(String desc) {
        throw new CustomException(ResultCode._INSERT_REPEAT_ERROR,desc == null ? ResultCode._INSERT_REPEAT_ERROR.getMessage() : desc);
    }

    // 更新结果失败异常
    default void updateError(String desc) {
        throw new CustomException(ResultCode._UPDATE_ERROR,desc == null ? ResultCode._UPDATE_ERROR.getMessage() : desc);
    }

    // 类型转换失败
    default void typeConvertError(String desc) {
        throw new CustomException(ResultCode._TYPE_CONVERT_ERROR,desc == null ? ResultCode._TYPE_CONVERT_ERROR.getMessage() : desc);
    }
}
