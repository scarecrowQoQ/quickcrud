package com.tml.quickcrud.template.domain;

import com.tml.quickcrud.template.anno.PropertyView;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PropertyInfo {
    // 属性名
    private String propertyName;

    // 属性类型
    private PropertyView.PropertyType propertyType;

    // 属性中文名
    private String propertyCNName;

    // 属性分组id
    private Integer groupId;

    // 属性分组名称
    private String groupName;

    // 输入提示
    private String inputTips;

    // 是否允许为空
    private Boolean notEmpty;

    // 最大长度
    private Integer maxLen;

    // 最小长度
    private Integer minLen;

    // 下拉框选项
    private List<OptionItem> options;

    // 是否在查询条件中展示
    private Boolean selectParamShow;

    // 是否在表格中展示
    private Boolean tableShow;

    // 是否在添加表单中展示
    private Boolean insertFormShow;

    // 是否在编辑表单中展示
    private Boolean updateFormShow;

    // 是否为自定义组件
    private Boolean customModule;

    private FormRule formRule;

    public PropertyInfo(PropertyView propertyView) {
        this.propertyCNName = propertyView.CNName();
        this.groupId = propertyView.groupId();
        this.inputTips = propertyView.inputTips();
        this.selectParamShow = propertyView.selectParamShow();
        this.tableShow = propertyView.tableShow();
        this.insertFormShow = propertyView.insertFormShow();
        this.updateFormShow = propertyView.updateFormShow();
        this.customModule = propertyView.skipParsing();
        this.options = new ArrayList<>();
        this.formRule = new FormRule();
    }
    // 下拉框选项类
    @Data
    public static class OptionItem {
        private String label;
        private Integer value;
    }

}
