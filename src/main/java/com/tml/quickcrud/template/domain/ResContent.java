package com.tml.quickcrud.template.domain;

import lombok.Data;

@Data
public class ResContent {
    private Integer code;
    private String message;
    private Object data;

    public static ResContent success(Object data){
        ResContent resContent = new ResContent();
        resContent.setCode(200);
        resContent.setMessage("success");
        resContent.setData(data);
        return resContent;
    }

    public static ResContent fail(String message){
        ResContent resContent = new ResContent();
        resContent.setCode(500);
        resContent.setMessage(message);
        return resContent;
    }

}
