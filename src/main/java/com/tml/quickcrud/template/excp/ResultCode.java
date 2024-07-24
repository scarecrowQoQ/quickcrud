package com.tml.quickcrud.template.excp;

public enum ResultCode {
    SUCCESS(200, "成功"),

    ERROR(400, "服务器繁忙，请稍后重试"),

    USERNAME_EXIST(1001,"用户名被注册"),

    USER_EMAIL_EXIST(1002,"用户邮箱被注册"),

    User_Password_Error(1003,"用户密码错误"),

    User_Username_NotExist(1003,"用户账户不存在"),

    Token_MISS(2001,"TOKEN令牌不能为空"),

    _PARAMETER_ILLEGAL(3001,"入参属性值非法异常"),

    _RECORD_NOT_EXIST(3002,"查询结果为空"),

    _INSERT_ERROR(3003,"插入结果失败异常"),

    _INSERT_REPEAT_ERROR(3004,"重复插入结果失败"),

    _UPDATE_ERROR(3005,"更新结果失败异常"),

    _TYPE_CONVERT_ERROR(3006,"类型转换失败");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }
    public Integer getCode(){
        return this.code;
    }

    @Override
    public String toString() {
        return "{ResultCode: message="+this.message+",code="+this.getCode()+"}";
    }
}
