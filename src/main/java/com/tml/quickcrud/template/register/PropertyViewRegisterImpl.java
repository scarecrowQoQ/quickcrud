package com.tml.quickcrud.template.register;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import com.tml.quickcrud.template.anno.OptionLabel;
import com.tml.quickcrud.template.anno.OptionValue;
import com.tml.quickcrud.template.anno.PropertyView;
import com.tml.quickcrud.template.anno.SelectType;
import com.tml.quickcrud.template.domain.BaseEnum;
import com.tml.quickcrud.template.domain.BaseRecordInfo;
import com.tml.quickcrud.template.domain.FormRule;
import com.tml.quickcrud.template.domain.PropertyInfo;
import com.tml.quickcrud.template.util.StringUtilsExt;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.*;

@Data
@Slf4j
public class PropertyViewRegisterImpl implements PropertyViewRegister {

    /**
     * Po字段视图映射
     *  对于po下的其他对象嵌套，map进行扁平化处理，对象属性将以同一个groupName进行展示来代表一个对象
     *  key: 类名.字段名 因为po下可能存在多个对象引用而对象引用之间可能存在相同属性名，使用类名作为前缀进行区分,根属性不添加，即Po省略
     *  value：所有字段只要添加了@PropertyView注解就将会被注册在此map中
     */
    private final HashMap<String, PropertyInfo> poPropertyMap = new HashMap<>();

    // 查询视图配置
    private final List<PropertyInfo> selectConfig = new ArrayList<>();
    // 结果视图配置
    private final List<PropertyInfo> resultViewConfig = new ArrayList<>();
    // 插入的视图配置
    private final List<PropertyInfo> insertConfig = new ArrayList<>();
    // 更新的视图配置
    private final List<PropertyInfo> updateConfig = new ArrayList<>();
    // 主键字段名
    private String tableIdFieldName;
    // 删除字段名
    private String delColumnName;
    // 有效字段名
    private String validColumnName;
    // 排序字段名
    private String sortColumnName;
    // Po属性名与数据库字段映射
    private Map<String,String> poFieldToDbFieldMapper = new HashMap<>();
    // OptionLabel与value属性名
    private String optionsLabelPropertyName, optionsValuePropertyName;
    // 表单规则
    private FormRule formRule = new FormRule();

//    private SysDictDataMapper dictDataMapper;
    /**
     * 注册Po字段信息
     * @param po po类
     */
    public <Po extends BaseRecordInfo> PropertyViewRegisterImpl(Class<Po> po ){
//        this.dictDataMapper = SpringUtils.getBean(SysDictDataMapper.class);
        this.handleRegisterPropertyInfo(po, "", null);
        // 日志打印，检查字段内容是否正确
        for (Map.Entry<String, PropertyInfo> stringPropertyInfoEntry : this.poPropertyMap.entrySet()) {
            log.debug("key：{}，value：{}",stringPropertyInfoEntry.getKey(),stringPropertyInfoEntry.getValue());
        }
        // 获取po的基本字段信息
        try {
            Po p = po.getDeclaredConstructor().newInstance();
            delColumnName = p.initDelField();
            validColumnName = p.initValidField();
            sortColumnName = p.initSortModeField();
        } catch (Exception e) {
            log.error("初始化BaseRecordInfo失败, 错误信息:{}",e.getMessage());
        }
    }

    private void handleRegisterPropertyInfo(Class<?> clazz, String prefix, PropertyView lastPropertyView) {
        List<Field> list = new ArrayList<>();
        Class<?> tempClass = clazz;
        // 循环获取父类class对象的Fields,遇到Object类则停止，作用是读取父类中的公共字段信息
        while (tempClass != null && !tempClass.getName().equalsIgnoreCase("java.lang.object")){
            Field[] declaredFields = tempClass.getDeclaredFields();
            list.addAll(Arrays.asList(declaredFields));
            tempClass = tempClass.getSuperclass();
        }
        // 遍历字段，读取注解信息,同时需要获取字段与属性名的映射
        for (Field declaredField : list) {
            declaredField.setAccessible(true);
            if (declaredField.getDeclaredAnnotation(TableId.class) != null) {
                TableId tableId = declaredField.getDeclaredAnnotation(TableId.class);
                String dbFieldName = tableId.value().isEmpty() ? StringUtilsExt.toUnderScoreCase(declaredField.getName()) : tableId.value();
                poFieldToDbFieldMapper.put(prefix+declaredField.getName(),dbFieldName);
                this.tableIdFieldName = dbFieldName;
            }
            else if(declaredField.getDeclaredAnnotation(TableField.class) != null){
                TableField tableField = declaredField.getDeclaredAnnotation(TableField.class);
                String dbFieldName = tableField.value().isEmpty() ? StringUtilsExt.toUnderScoreCase(declaredField.getName()) : tableField.value();
                poFieldToDbFieldMapper.put(prefix+declaredField.getName(),dbFieldName);
            }else {
                poFieldToDbFieldMapper.put(prefix+declaredField.getName(),StringUtilsExt.toUnderScoreCase(declaredField.getName()));
            }
            if(declaredField.getDeclaredAnnotation(OptionLabel.class) != null){
                optionsLabelPropertyName = declaredField.getName();
            }
            if(declaredField.getDeclaredAnnotation(OptionValue.class) != null){
                optionsValuePropertyName = declaredField.getName();
            }
            // 对于没有注解配置的字段，不再进行下面解析的处理
            PropertyView propertyView = declaredField.getDeclaredAnnotation(PropertyView.class);
            if (propertyView == null || propertyView.skipParsing()) {
                continue;
            }
            // 读取注解信息并实例化属性信息对象
            PropertyInfo propertyInfo = new PropertyInfo(propertyView);
            // 设置属性名，前缀+字段名
            propertyInfo.setPropertyName(prefix + declaredField.getName());
            switch (propertyView.type()) {
                // 递归处理引用类型
                case Object:
                    this.handleRegisterPropertyInfo(propertyView.reference(), prefix + declaredField.getName() + ".", propertyView);
                    break;
                // json对象
                case Json:
                    propertyInfo.setPropertyType(PropertyView.PropertyType.String);
                    this.handleRegisterPropertyInfo(propertyView.reference(), prefix + declaredField.getName() + ".", propertyView);
                    break;
                // 表单槽类型
                case FormSlot:
                    propertyInfo.setPropertyType(PropertyView.PropertyType.FormSlot);
                    break;
                // map对象则实例化对象，解析map中的key作为属性名与中文名
                case Map:
                    try {
                        String name = declaredField.getName();
                        Object obj = clazz.getDeclaredConstructor().newInstance();
                        HashMap<String, Object> map = (HashMap) declaredField.get(obj);
                        for (String s : map.keySet()) {
                            PropertyInfo propertyInfo1 = new PropertyInfo();
                            propertyInfo1.setPropertyName(prefix + name + "." + s);
                            propertyInfo1.setPropertyCNName(s);
                            propertyInfo1.setPropertyType(PropertyView.PropertyType.String);
                            propertyInfo1.setGroupId(propertyView.groupId());
                            propertyInfo1.setGroupName(lastPropertyView.CNName());
                            this.poPropertyMap.put(prefix + name + "." + s, propertyInfo1);
                        }
                    } catch (Exception e) {
                        log.error("[模板解析] 解析Map类型{}字段失败，实例化Map失败，请检查是否为Map类型或者有无参构造，错误信息：{}", prefix + declaredField.getName(), e.getMessage());
                    }
                    break;
                case Select:
                    try {
                        String referenceName = propertyView.referenceName();
                        // 如果propertyView.referenceName() 是以com.ly开头则认为是枚举类
                        if(referenceName.startsWith("com.ly")){
                            Class<?> referenceClass = Class.forName(propertyView.referenceName());
                            // 枚举类必须按照继承BaseEnum接口才可解析
                            if (BaseEnum.class.isAssignableFrom(referenceClass) && referenceClass.isEnum()) {
                                // 因为已经确认referenceClass是枚举类，所以可以安全地转换
                                Class<? extends Enum> enumClass = (Class<? extends Enum>) referenceClass;
                                // 获取所有枚举实例
                                Object[] enumConstants = enumClass.getEnumConstants();
                                List<PropertyInfo.OptionItem> options = propertyInfo.getOptions();
                                for (Object enumConstant : enumConstants) {
                                    BaseEnum baseEnum = (BaseEnum) enumConstant;
                                    PropertyInfo.OptionItem optionItem = new PropertyInfo.OptionItem();
                                    optionItem.setLabel(baseEnum.getDesc().toString());
                                    optionItem.setValue(baseEnum.getCode());
                                    options.add(optionItem);
                                }
                            }else{
                                log.error("[模板解析] Select类型解析错误，必须为枚举且继承BaseEnum接口，属性名：{}",propertyInfo.getPropertyName());
                            }
                        }
//                        else{
//                            // 如果不是枚举类，则认为是字典表
//                            SysDictData dictData = new SysDictData();
//                            dictData.setDictType(referenceName);
//                            List<SysDictData> sysDictDataList = dictDataMapper.selectDictDataList(dictData);
//                            if(sysDictDataList.isEmpty()){
//                                log.warn("[模板解析] 字典表{}为空，请检查字典表是否存在数据",referenceName);
//                            }
//                            for (SysDictData temp : sysDictDataList) {
//                                PropertyInfo.OptionItem optionItem = new PropertyInfo.OptionItem();
//                                optionItem.setLabel(temp.getDictLabel());
//                                optionItem.setValue(Integer.valueOf(temp.getDictValue()));
//                                propertyInfo.getOptions().add(optionItem);
//                            }
//                        }
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
            }
            if(propertyInfo.getPropertyType() == null){
                propertyInfo.setPropertyType(propertyView.type());
            }
            // propertyInfo.setGroupId(propertyView.groupId());
            // 当前不是第一次递归,则以上一个字段的中文名作为组名
            if (lastPropertyView != null) {
                propertyInfo.setGroupName(lastPropertyView.CNName());
            } else {
                propertyInfo.setGroupName(propertyView.groupName());
            }
            this.poPropertyMap.put(prefix+declaredField.getName(),propertyInfo);
            declaredField.setAccessible(false);
        }
        log.info("[模板解析] {}类字段注册完成，总共：{}个字段",clazz.getName(),poPropertyMap.size());
    }

    @Override
    public <reqDto> List<PropertyInfo> selectConfigOutput(Class<reqDto> reqDto) {
        handleSelectConfigOutput(reqDto, "");
        return this.selectConfig;
    }
    private void handleSelectConfigOutput(Class<?> clazz, String prefix) {
        for (Field declaredField : clazz.getDeclaredFields()) {
            // 如果没有@SelectType注解,或者该字段不需要在前端进行展示输入，则跳过
            SelectType selectType = declaredField.getDeclaredAnnotation(SelectType.class);
            if (selectType == null) {
                continue;
            }
            String name = prefix+declaredField.getName();
            PropertyInfo propertyInfo = poPropertyMap.get(name);
            // 如果没有@PropertyView注解，或者配置了查询表单不展示则跳过
            if (propertyInfo == null || !propertyInfo.getSelectParamShow()) {
                continue;
            }
            if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Object) {
                // 如果是对象类型，获取该对象名并作为下一个递归的前缀，然后进行递归其属性
                handleSelectConfigOutput(declaredField.getType(),prefix + declaredField.getName()+".");
                // 对象本身将不会作为一个字段显示，而是以一个组名展示
                continue;
            }
            this.selectConfig.add(propertyInfo);
        }
    }

    @Override
    public <resDto> List<PropertyInfo> resultViewConfigOutput(Class<resDto> resDto) {
        handleResultViewConfigOutPut(resDto, "");
        return this.resultViewConfig;
    }

    private void handleResultViewConfigOutPut(Class<?> clazz, String prefix){
        for (Field declaredField : clazz.getDeclaredFields()) {
            declaredField.setAccessible(true);
            String name = prefix+declaredField.getName();
            PropertyInfo propertyInfo = poPropertyMap.get(name);
            // 如果没有@PropertyView注解，或者配置了表格不展示则跳过
            if (propertyInfo == null) {
                continue;
            }
            if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Object) {
                // 如果是对象类型，获取该对象名并作为下一个递归的前缀，然后进行递归其属性
                this.handleResultViewConfigOutPut(declaredField.getType(),prefix + declaredField.getName()+".");
                // 对象本身将不会作为一个字段显示，而是以一个组名展示
                continue;
            }
            else if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Map){
                try {
                    Object obj = clazz.getDeclaredConstructor().newInstance();
                    Map<String,Object> map = (Map) declaredField.get(obj);
                    for (String key : map.keySet()) {
                        String s = name+"."+key;
                        propertyInfo = poPropertyMap.get(s);
                        this.resultViewConfig.add(propertyInfo);
                    }
                } catch (Exception e) {
                    log.error("[模板解析] 解析Map类型{}字段失败，实例化Map失败，请检查是否为Map类型或者有无参构造，错误信息：{}",name,e.getMessage());
                }
            }else if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Select){
                this.resultViewConfig.add(propertyInfo);
            }
            else {
                this.resultViewConfig.add(propertyInfo);
            }
            declaredField.setAccessible(false);
        }
    }

    @Override
    public <reqDto> List<PropertyInfo> insertConfigOutput(Class<reqDto> reqDto) {
        handleInsertConfigOutput(reqDto, "");
        return this.insertConfig;
    }

    private void handleInsertConfigOutput(Class<?> clazz, String prefix){
        for (Field declaredField : clazz.getDeclaredFields()) {
            declaredField.setAccessible(true);
            String name = prefix + declaredField.getName();
            PropertyInfo propertyInfo = poPropertyMap.get(name);
            // 如果没有@PropertyView注解，或者配置了表单不展示则跳过
            if (propertyInfo == null || !propertyInfo.getInsertFormShow()) {
                continue;
            }
            if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Object) {
                // 如果是对象类型，获取该对象名并作为下一个递归的前缀，然后进行递归其属性
                this.handleInsertConfigOutput(declaredField.getType(),prefix + declaredField.getName()+".");
                // 对象本身将不会作为一个字段显示，而是以一个组名展示
                continue;
            }
            else if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Map){
                try {
                    Object obj = clazz.getDeclaredConstructor().newInstance();
                    HashMap<String,Object> map = (HashMap) declaredField.get(obj);
                    for (String key : map.keySet()) {
                        String s = name+"."+key;
                        propertyInfo = poPropertyMap.get(s);
                        this.insertConfig.add(propertyInfo);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                this.insertConfig.add(propertyInfo);
            }
            // 以下为输入规则配置
            inputRuleConfig(declaredField, propertyInfo);
            declaredField.setAccessible(false);
        }
    }

    @Override
    public <reqDto> List<PropertyInfo> updateConfigOutput(Class<reqDto> reqDto) {
        handleUpdateConfigOutPut(reqDto, "");
        return this.updateConfig;
    }

    private void handleUpdateConfigOutPut(Class<?> clazz, String prefix){
        for (Field declaredField : clazz.getDeclaredFields()) {
            declaredField.setAccessible(true);
            String name = prefix+declaredField.getName();
            PropertyInfo propertyInfo = poPropertyMap.get(name);
            // 如果没有@PropertyView注解，或者配置了表单不展示则跳过
            if (propertyInfo == null || !propertyInfo.getUpdateFormShow()) {
                continue;
            }
            if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Object) {
                // 如果是对象类型，获取该对象名并作为下一个递归的前缀，然后进行递归其属性
                this.handleUpdateConfigOutPut(declaredField.getType(),prefix + declaredField.getName()+".");
                // 对象本身将不会作为一个字段显示，而是以一个组名展示
                continue;
            }else if (propertyInfo.getPropertyType() == PropertyView.PropertyType.Map){
                try {
                    Object obj = clazz.getDeclaredConstructor().newInstance();
                    HashMap<String,Object> map = (HashMap) declaredField.get(obj);
                    for (String key : map.keySet()) {
                        String s = name+"."+key;
                        propertyInfo = poPropertyMap.get(s);
                        this.updateConfig.add(propertyInfo);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }else{
                this.updateConfig.add(propertyInfo);
            }
            // 以下为输入规则配置
            inputRuleConfig(declaredField, propertyInfo);
            declaredField.setAccessible(false);
        }
    }

    private void inputRuleConfig(Field declaredField, PropertyInfo propertyInfo) {
        FormRule.Rule rule = new FormRule.Rule();
        if(declaredField.getDeclaredAnnotation(Length.class) != null){
            Length length = declaredField.getDeclaredAnnotation(Length.class);
            propertyInfo.setMaxLen(length.max());
            propertyInfo.setMinLen(length.min());
        }
        if(declaredField.getDeclaredAnnotation(NotEmpty.class) != null
                || declaredField.getDeclaredAnnotation(NotBlank.class) != null
                || declaredField.getDeclaredAnnotation(NotNull.class) != null){
            propertyInfo.setNotEmpty(true);
            rule.setRequired(true);
            rule.setMessage(propertyInfo.getPropertyCNName()+"不能为空");
            rule.setTrigger(FormRule.Trigger.blur);
        }else{
            propertyInfo.setNotEmpty(false);
            rule.setRequired(false);
        }
        if(propertyInfo.getMaxLen() == null){
            propertyInfo.setMaxLen(Integer.MAX_VALUE);
        }
        if(propertyInfo.getMinLen() == null){
            if(propertyInfo.getNotEmpty()){
                propertyInfo.setMinLen(1);
            }else{
                propertyInfo.setMinLen(0);
            }
        }
        // 这里需要将嵌套对象的属性名的.进行替换为_，因为前端解析表单规则会将'var1.var2'解析为对象的属性而不是字符串
        formRule.getRules().put(propertyInfo.getPropertyName().replace('.','_'), Collections.singletonList(rule));
    }
}