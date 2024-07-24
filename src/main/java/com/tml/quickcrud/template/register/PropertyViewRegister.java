package com.tml.quickcrud.template.register;


import com.tml.quickcrud.template.domain.FormRule;
import com.tml.quickcrud.template.domain.PropertyInfo;

import java.util.List;
import java.util.Map;

public interface PropertyViewRegister {
    /**
     * 以下四个为前端字段视图配置
     */
    public <reqDto> List<PropertyInfo> selectConfigOutput(Class<reqDto> reqDto);
    public <resDto> List<PropertyInfo> resultViewConfigOutput(Class<resDto> resDto);
    public <reqDto> List<PropertyInfo> insertConfigOutput(Class<reqDto> reqDto);
    public <reqDto> List<PropertyInfo> updateConfigOutput(Class<reqDto> reqDto);
    public String getTableIdFieldName();
    String getDelColumnName();
    String getValidColumnName();
    String getSortColumnName();
    Map getPoFieldToDbFieldMapper();
    String getOptionsValuePropertyName();
    String getOptionsLabelPropertyName();
    FormRule getFormRule();
}