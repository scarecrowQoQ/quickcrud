package com.tml.quickcrud.template.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.tml.quickcrud.template.anno.SelectType;
import com.tml.quickcrud.template.domain.*;
import com.tml.quickcrud.template.excp.BaseExceptionTemplate;
import com.tml.quickcrud.template.register.PropertyViewRegister;
import com.tml.quickcrud.template.register.PropertyViewRegisterImpl;
import com.tml.quickcrud.template.util.CacheClientHA;
import com.tml.quickcrud.template.util.StringUtilsExt;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用业务模板
 * 1. 自动的RequestDto与Po与ResponseDto的转换，可以自定义规则
 * 2. 自动的更新Po操作记录
 * 3. 自动一般业务逻辑异常抛出与日志打印
 * 4. 查询，结果，插入or更新 的视图配置返回
 * @param <M> 为mapper,继承BaseMapper
 * @param <P> 为po,继承BaseRecordInfo,方便自动更新记录
 * @param <Req> 为请求dto,继承PaginationEntity,方便分页查询
 * @param <Res> 为返回dto
 */
public class BaseServiceDefaultImpl <M extends BaseMapper<P>, P extends BaseRecordInfo,Req extends PaginationEntity,Res>
        extends ServiceImpl<M, P>
        implements BaseServiceTemplate<P,Req,Res> , BaseExceptionTemplate, CommandLineRunner {
    // po的class缓存
    private Class<P> poClazz = null;

    // requestDto的class缓存
    private Class<Req> requestDtoClazz = null;

    // responseDto的class缓存
    private Class<Res> responseDtoClazz = null;

    // 查询视图配置
    @Getter
    private List<PropertyInfo> selectConfig;

    // 结果视图配置
    @Getter
    private List<PropertyInfo> resultViewConfig;

    // 插入的视图配置
    @Getter
    private List<PropertyInfo> insertConfig;

    // 更新的视图配置
    @Getter
    private List<PropertyInfo> updateConfig;

    // 主键属性名
    private String tableIdFieldName;

    // 删除字段名
    private String delColumnName;

    // 有效字段名
    private String validColumnName;

    // 默认排序字段
    private String defaultSortField;

    // OptionLabel与value属性名
    private String optionsLabelPropertyName, optionsValuePropertyName;

    // 更新忽略字段
    protected final Set<String> insertAndUpdateIgnoreFields = new HashSet<>();

    {
        insertAndUpdateIgnoreFields.add("serial_version_uid");
        insertAndUpdateIgnoreFields.add("log");
        insertAndUpdateIgnoreFields.add("created_date");
        insertAndUpdateIgnoreFields.add("update_date");
        insertAndUpdateIgnoreFields.add("created_by");
        insertAndUpdateIgnoreFields.add("update_by");
    }

    // Po属性名与数据库字段映射
    private Map<String,String> poFieldToDbFieldMapper = new HashMap<>();

    private FormRule formRule;

    protected Logger log = LoggerFactory.getLogger(this.getClass());



    /**
     * 构造返回ResponseDto并赋值Po属性值
     */
    private Res generateResponseDto(P p){
        if(p == null){
            log.error("Po为空，无法生成ResponseDto，当前Po类：{}", this.poClazz.getName());
            return null;
        }
        Res res;
        try {
            res = this.responseDtoClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("ResponseDto 实例化失败:{}", e.getMessage());
            return null;
        }
        this.defaultPoConvertRes(p, res);
        return res;
    }

    // 批量Po转ResponseDto列表
    private List<Res> generateResponseDto(List<P> p){
        return p.stream().map(this::generateResponseDto).collect(Collectors.toList());
    }

    private P generatePoInstance(){
        try {
            return this.poClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Po 实例化失败:{}", e.getMessage());
            this.typeConvertError("Po实例化失败,请检查Po是否正确实现无参构造函数或者defaultReqConvertPo方法是否正确重写");
        }
        return null;
    }


    // 缓存删除
    protected void cleanCache(P p){
        String cacheKey = this.getCacheKey(p);
        if(StringUtilsExt.isNotEmpty(cacheKey)){
            CacheClientHA cacheClient = this.getCacheClient();
            if(cacheClient != null){
                this.getCacheClient().Key().del(cacheKey);
            }else{
                log.error("缓存客户端为空，无法删除缓存");
            }
        }
    }

    @Override
    // 添加模板
    public Boolean insertMustNotExist(Req requestDto) {
        if (defaultSelectByDto(requestDto) != null) {
            log.error("{},该记录已存在，插入失败!", requestDto);
            this.insertRepeatError("已有相同记录，请勿重复添加");
            return false;
        }
        P p = generatePoInstance();
        this.defaultReqConvertPo(requestDto, p);
        assert p != null;
        p.initializeBaseRecordInfo();
        boolean save = super.save(p);
        return save;
    }

    @Override
    public Boolean insertBatch(List<Req> requestDtoList) {
        List<P> pList = new ArrayList<>();
        for (Req req : requestDtoList) {
            P p = generatePoInstance();
            this.defaultReqConvertPo(req, p);
            assert p != null;
            p.initializeBaseRecordInfo();
            pList.add(p);
        }
        return super.saveBatch(pList);
    }

    @Override
    public Boolean insertPo(P po) {
        if(po == null){
            log.error("插入数据为空，插入失败!");
            this.recordNotExist("插入数据为空，插入失败!");
            return false;
        }
        po.initializeBaseRecordInfo();
        return super.save(po);
    }

    @Override
    public Boolean insertFromExcel(MultipartFile file) {
//        if(file == null){
//            log.error("文件为空，导入失败!");
//            this.recordNotExist("文件为空，导入失败!");
//            return false;
//        }
//        ExcelUtil<P> util = new ExcelUtil<P>(poClazz);
//        try {
//            List<P> pos = util.importExcel(file.getInputStream());
//            for (P po : pos) {
//                po.initializeBaseRecordInfo();
//            }
//            return super.saveBatch(pos);
//        } catch (IOException e) {
//            log.error("Excel文件读取失败:{}", e.getMessage());
//            return false;
//        }
        return false;
    }

    @Override
    //  更新业务模板
    public Boolean updateTemplate(Req requestDto) {
        P p = this.defaultSelectByDto(requestDto);
        if(p == null){
            log.error("待更新数据不存在! {}",requestDto);
            this.recordNotExist("待更新记录不存在，更新失败!");
            return false;
        }
        this.defaultReqConvertPo(requestDto, p);
        p.updateBaseRecordInfo();
        UpdateWrapper<P> pUpdateWrapper = this.defaultGetUpdateWrapper(p);
        if (!super.update(p,pUpdateWrapper)) {
            log.error("更新失败! {}",requestDto);
            return false;
        }
        cleanCache(p);
        return true;

    }

    @Override
    // 查询未被删除的记录列表-分页
    public PageSelectResWrapper<Res> selectByPageMustNotDel(Req requestDto) {
        int pageNum = requestDto.getPageNum();
        int pageSize = requestDto.getPageSize();
        if(pageNum <= 0 || pageSize <= 0){
            log.error("分页数据无效，请检查分页数据");
            this.recordNotExist("分页数据无效");
        }
        Page<P> page = new Page<>(requestDto.getPageNum(), requestDto.getPageSize());
        defaultSortMode(page);
        // 通过遍历requestDto字段上的@SelectType 来确定查询字段和逻辑
        QueryWrapper<P> columnNameAndValue = defaultGetQueryWrapper(requestDto);
        super.page(page,columnNameAndValue);
        List<P> records = page.getRecords();
        return new PageSelectResWrapper<>( page.getTotal(),generateResponseDto(records));
    }

    @Override
    public PageSelectResWrapper<Res> selectByPageMustNotDel(Req requestDto, String sortField, String sortType) {
        String columnName = poFieldToDbFieldMapper.getOrDefault(sortField, StringUtilsExt.toUnderScoreCase(sortField));
        Page<P> page = new Page<>(requestDto.getPageNum(), requestDto.getPageSize());
        if(sortType.equals("desc")){
            page.addOrder(OrderItem.desc(columnName));
        }else if(sortType.equals("asc")){
            page.addOrder(OrderItem.asc(columnName));
        }
        QueryWrapper<P> columnNameAndValue = defaultGetQueryWrapper(requestDto);
        super.page(page,columnNameAndValue);
        List<P> records = page.getRecords();
        return new PageSelectResWrapper<>(page.getTotal(),generateResponseDto(records));
    }

    @Override
    public List<Res> selectListMustNotDel(Req requestDto) {
        QueryWrapper<P> columnNameAndValue = defaultGetQueryWrapper(requestDto);
        List<P> list = super.list(columnNameAndValue);
        List<P> collect = list.stream().filter(p -> !p.isDel()).collect(Collectors.toList());
        return generateResponseDto(collect);
    }

    // 指定排序字段以及排序方法
    @Override
    public List<Res> selectListMustNotDelWithSortField(Req requestDto, String sortField, String sortType) {
        String columnName = poFieldToDbFieldMapper.getOrDefault(sortField, StringUtilsExt.toUnderScoreCase(sortField));
        QueryWrapper<P> columnNameAndValue = defaultGetQueryWrapper(requestDto);
        if(sortType.equals("desc")){
            columnNameAndValue.orderByDesc(columnName);
        }else if(sortType.equals("asc")){
            columnNameAndValue.orderByAsc(columnName);
        }
        if(StringUtilsExt.isNotEmpty(delColumnName)){
            columnNameAndValue.eq(delColumnName,0);
        }
        List<P> list = super.list(columnNameAndValue);
        return generateResponseDto(list);
    }

    // 指定查询字段
    @Override
    public List<Res> selectListMustNotDelWithPoFields(Req requestDto, String... selectFields) {
        if(selectFields.length == 0) {
            log.error("查询字段为空，请指定查询字段。查询类:{}", this.requestDtoClazz.getName());
            this.recordNotExist("查询字段为空!请指定查询字段");
        }
        for (int i = 0; i < selectFields.length; i++) {
            String selectField = selectFields[i];
            // 查询字段检查
            if( ! poFieldToDbFieldMapper.containsKey(selectField)){
                log.error("查询字段{}不存在，请检查查询字段是否正确。查询类:{}", selectField,this.requestDtoClazz.getName());
                this.recordNotExist("查询字段不存在，请检查查询字段是否正确，确保为Po属性名而非DB字段名");
            }
            String dbField = poFieldToDbFieldMapper.get(selectField);
            selectFields[i] = dbField + " as " + selectField;
        }
        QueryWrapper<P> columnNameAndValue = defaultGetQueryWrapper(requestDto);
        columnNameAndValue.select(selectFields);
        List<P> list = super.list(columnNameAndValue);
        if(list.isEmpty()){
            return new ArrayList<>();
        }
        return generateResponseDto(list);
    }

    @Override
    public List<Map> selectOptionList(Req requestDto) {
        if (!StringUtilsExt.isNotEmpty(optionsValuePropertyName) || !StringUtilsExt.isNotEmpty(optionsLabelPropertyName)) {
            log.error("OptionLabel与value属性名未配置，请检查配置,标注OptionLabel与optionValue注解。查询类:{}", this.requestDtoClazz.getName());
            return new ArrayList<>(); // 配置错误，返回空列表
        }

        List<Res> res = selectListMustNotDelWithPoFields(requestDto, optionsValuePropertyName, optionsLabelPropertyName);
        if (res.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map> options = new ArrayList<>();
        for (Res item : res) {
            Map<String, Object> option = new HashMap<>();
            try {
                // 获取 value 属性的值
                String valueGetterName = "get" + Character.toUpperCase(optionsValuePropertyName.charAt(0)) + optionsValuePropertyName.substring(1);
                Method valueGetter = item.getClass().getMethod(valueGetterName);
                Object value = valueGetter.invoke(item);

                // 获取 label 属性的值
                String labelGetterName = "get" + Character.toUpperCase(optionsLabelPropertyName.charAt(0)) + optionsLabelPropertyName.substring(1);
                Method labelGetter = item.getClass().getMethod(labelGetterName);
                Object label = labelGetter.invoke(item);

                option.put("value", value);
                option.put("label", label);
                options.add(option);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("无法访问Getter方法或方法调用失败。错误：{}", e.getMessage());
            }
        }
        return options;
    }


    // 根据id查询未被删除的记录
    @Override
    public Res selectByIdMustNotDel(Serializable id) {
        P p = super.getById(id);
        if(p == null || p.isDel() ){
            return null;
        }
        return generateResponseDto(p);
    }

    // 根据id查询未被删除并生效的记录
    @Override
    public Res selectByIdMustNotDelAndIsValid(Serializable id) {
        if(id == null){
            log.error("id查询参数为空，查询类:{}",this.requestDtoClazz.getName());
            this.recordNotExist("id查询的id不可为空!");
        }
        P p = super.getById(id);
        if(p == null || p.isDel() || p.isValid()){
            return null;
        }
        return generateResponseDto(p);
    }

    @Override
    public Res selectOneMustNotDelAndMultiple(Req dto) {
        List<Res> res = selectListMustNotDel(dto);
        if(res.size() > 1){
            log.error("查询结果不唯一，查询类:{}",this.requestDtoClazz.getName());
            throw new RuntimeException("查询结果不唯一");
        }
        return res.get(0);
    }

    @Override
    // 默认Dto查询逻辑为主键匹配
    public P defaultSelectByDto(Req obj){
        for (Field declaredField : this.requestDtoClazz.getDeclaredFields()) {
            if ( poFieldToDbFieldMapper.get(declaredField.getName()).equals(this.tableIdFieldName)) {
                declaredField.setAccessible(true);
                try {
                    Serializable id = (Serializable) declaredField.get(obj);
                    if(id != null){
                        return super.getById(id);
                    }
                } catch (IllegalAccessException e) {
                    log.error("获取属性值失败:{}", e.getMessage());
                }
            }
        }
        log.error("{}：未找到主键字段，无法进行主键查询。请检查Req对象是否有主键字段。",this.requestDtoClazz.getName());
        return null;
    }

    @Override
    public P defaultSelectById(Serializable id) {
        P p = super.getById(id);
        if(p == null || p.isDel()){
            log.error("待查询元素已被删除或不存在!");
            this.recordNotExist("待查询元素已被删除或不存在!");
        }
        return p;
    }

    // 删除模板-必须存在或未被删除
    @Override
    public Boolean deleteMustExist(Req requestDto){
        P p = this.defaultSelectByDto(requestDto);
        if (p == null || p.isDel()){
            log.error("待删除元素已被删除或不存在。类:{}",requestDto.getClass().getName());
            this.recordNotExist("待删除元素已被删除或不存在!");
            return false;
        }
        p.setDel(true);
        p.updateBaseRecordInfo();
        super.updateById(p);
        cleanCache(p);
        return true;
    }

    // 删除模板
    @Override
    public Boolean deleteTemplate(Req requestDto){
        P p = this.defaultSelectByDto(requestDto);
        if(p == null){
            return true;
        }
        p.setDel(true);
        p.updateBaseRecordInfo();
        if(super.updateById(p)){
            // 检查是否需要删除缓存
            this.getCacheKey(p);
            return true;
        }
        return false;
    }

    @Override
    public Boolean deleteByIdMustExist(Serializable id) {
        P p = this.defaultSelectById(id);
        if(p != null){
            p.setDel(true);
            p.updateBaseRecordInfo();
            if(super.updateById(p)){
                // 检查是否需要删除缓存
                cleanCache(p);
                return true;
            }else{
                updateError("删除失败");
                return false;
            }
        }else {
            recordNotExist("待删除的元素不存在! 记录id："+id.toString());
            return false;
        }
    }

    @Override
    public Boolean setValidBeTrueById(Serializable id) {
        P p = this.defaultSelectById(id);
        if(p == null) {
            recordNotExist("待生效的元素不存在! 记录id："+id.toString());
            return false;
        }
        p.setValid(true);
        p.updateBaseRecordInfo();
        if(super.updateById(p)){
            // 检查是否需要删除缓存
            cleanCache(p);
            return true;
        }
        updateError("生效失败");
        return false;
    }

    @Override
    public Boolean setValidBeFalseById(Serializable id) {
        P p = this.defaultSelectById(id);
        if(p == null) {
            recordNotExist("待失效的元素不存在! 记录id："+id.toString());
            return false;
        }
        p.setValid(false);
        p.updateBaseRecordInfo();
        if(super.updateById(p)){
            cleanCache(p);
            return true;
        }
        updateError("失效失败");
        return false;
    }

    @Override
    // 默认复制属性逻辑，requestDto转Po， String类型为null则填充""
    public void defaultReqConvertPo(Req requestDto, P p){
        BeanUtils.copyProperties(requestDto, p);
        for (Field declaredField : p.getClass().getDeclaredFields()) {
            declaredField.setAccessible(true);
            try {
                if (declaredField.getType() == String.class && declaredField.get(p) == null) {
                    declaredField.set(p,"");
                }
            } catch (IllegalAccessException e) {
                log.error("获取属性值失败:{}", e.getMessage());
            }
        }
    }

    /**
     * 默认的查询条件生成器
     */
    @Override
    public QueryWrapper<P> defaultGetQueryWrapper(Req requestDto){
        QueryWrapper<P> queryWrapper = new QueryWrapper<>();
        Field[] declaredFields = requestDto.getClass().getDeclaredFields();
        try {
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                // 没有被@SelectType注解的字段不进行查询
                SelectType selectType = declaredField.getDeclaredAnnotation(SelectType.class);
                if (selectType == null) {
                    continue;
                }
                String declaredFieldName = declaredField.getName();
                // 驼峰转下划线
                String dbColumnName = poFieldToDbFieldMapper.getOrDefault(declaredFieldName, StringUtilsExt.toUnderScoreCase(declaredFieldName));
                Object propertyValue = declaredField.get(requestDto);
                // 忽略空值,以及默认忽略字段
                if(propertyValue == null || propertyValue.equals("") || insertAndUpdateIgnoreFields.contains(dbColumnName)){
                    continue;
                }
                // 特殊字段isValid，如果为-1则不进行查询
                if(StringUtilsExt.isNotEmpty(validColumnName) && dbColumnName.equals(validColumnName) && propertyValue.equals(-1)){
                    continue;
                }
                switch (selectType.selectMode()) {
                    case like:
                        queryWrapper.like(dbColumnName, "%" + propertyValue + "%");
                        break;
                    case eq:
                        queryWrapper.eq(dbColumnName, propertyValue);
                        break;
                    case ge:
                        queryWrapper.ge(dbColumnName, propertyValue);
                        break;
                    case le:
                        queryWrapper.le(dbColumnName, propertyValue);
                        break;
                }
            }
        } catch (IllegalAccessException e) {
            log.error("获取属性值失败:{}", e.getMessage());
        }
        if(StringUtilsExt.isNotEmpty(delColumnName)){
            queryWrapper.eq(delColumnName,0);
        }
        return queryWrapper;
    }

    @Override
    public UpdateWrapper<P> defaultGetUpdateWrapper(P po) {
        UpdateWrapper<P> updateWrapper = new UpdateWrapper<>();
        Field[] declaredFields = po.getClass().getDeclaredFields();
        try {
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                String declaredFieldName = declaredField.getName();
                String dbColumnName = poFieldToDbFieldMapper.getOrDefault(declaredFieldName, StringUtilsExt.toUnderScoreCase(declaredFieldName));
                Object propertyValue = declaredField.get(po);
                // 如果是主键那么设置判断条件
                if (dbColumnName.equals(this.tableIdFieldName)) {
                    updateWrapper.eq(dbColumnName,propertyValue);
                    continue;
                }
                // 忽略null值或者该字段被默认忽略
                if(propertyValue == null || insertAndUpdateIgnoreFields.contains(dbColumnName)){
                    continue;
                }
                updateWrapper.set(dbColumnName,propertyValue);
                declaredField.setAccessible(false);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return updateWrapper;
    }

    /**
     * 默认排序模式
     */
    @Override
    public void defaultSortMode(Page<P> page) {
        if (StringUtilsExt.isNotEmpty(defaultSortField)) {
            page.addOrder(OrderItem.desc(defaultSortField));
        }
    }

    @Override
    // 默认复制属性逻辑，Po转responseDto
    public void defaultPoConvertRes(P po,Res responseDto){
        BeanUtils.copyProperties(po, responseDto);
    }

    /**
     * 初始化泛型类的class，以及结果视图配置，查询视图配置，更新视图配置，插入视图配置
     */
    @Override
    public void run(String... args) throws Exception {
        Type genericSuperclass = this.getClass().getGenericSuperclass();
        // 获取泛型类的class
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            this.poClazz = (Class<P>) actualTypeArguments[1];
            this.requestDtoClazz = (Class<Req>) actualTypeArguments[2];
            this.responseDtoClazz = (Class<Res>) actualTypeArguments[3];
        }
        // 初始化一个视图配置注册器
        PropertyViewRegister propertyViewRegister = new PropertyViewRegisterImpl(this.poClazz);
        // 初始化查询配置，更新配置，插入配置，结果配置
        this.resultViewConfig = propertyViewRegister.resultViewConfigOutput(this.responseDtoClazz);
        this.updateConfig = propertyViewRegister.updateConfigOutput(this.requestDtoClazz);
        this.insertConfig = propertyViewRegister.insertConfigOutput(this.requestDtoClazz);
        this.selectConfig = propertyViewRegister.selectConfigOutput(this.requestDtoClazz);
        // 初始化主键字段名
        this.tableIdFieldName = propertyViewRegister.getTableIdFieldName() == null ? "id" : propertyViewRegister.getTableIdFieldName();
        // 初始化删除字段名
        this.delColumnName = propertyViewRegister.getDelColumnName();
        // 初始化有效字段名
        this.validColumnName = propertyViewRegister.getValidColumnName();
        // 初始化排序字段
        this.defaultSortField = propertyViewRegister.getSortColumnName();
        // 初始化Po与Db字段的映射
        this.poFieldToDbFieldMapper = propertyViewRegister.getPoFieldToDbFieldMapper();
        // 初始化Option的value与label
        this.optionsLabelPropertyName = propertyViewRegister.getOptionsLabelPropertyName();
        this.optionsValuePropertyName = propertyViewRegister.getOptionsValuePropertyName();
        this.formRule = propertyViewRegister.getFormRule();
    }
}
