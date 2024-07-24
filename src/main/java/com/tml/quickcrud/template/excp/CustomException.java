package com.tml.quickcrud.template.excp;

public class CustomException extends RuntimeException{
    private final ResultCode resultCode;

    private final String message;


    public CustomException(ResultCode resultCode, String message) {
        this.resultCode = resultCode;
        this.message = message;
    }
}
