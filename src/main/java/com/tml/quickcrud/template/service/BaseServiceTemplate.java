package com.tml.quickcrud.template.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tml.quickcrud.template.domain.PageSelectResWrapper;
import com.tml.quickcrud.template.util.CacheClientHA;
import org.springframework.web.multipart.MultipartFile;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface BaseServiceTemplate <P,Req,Res> {
    /**
     * 设置缓存Key
     */
    default String getCacheKey(P po){
        return "";
    }

    /**
     * 设置缓存客户端
     */
    default CacheClientHA getCacheClient(){
        return null;
    }

    /**
     * 插入模板
     */
    // 插入但必须不存在，否则异常
    public Boolean insertMustNotExist(Req requestDto);

    // 批量插入
    public Boolean insertBatch(List<Req> requestDtoList);

    // 插入Po
    public Boolean insertPo(P po);

    //从Excel中批量导入Po
    public Boolean insertFromExcel(MultipartFile file);

    /**
     * 更新模板
     */
    //  更新业务模板
    public Boolean updateTemplate(Req requestDto);

    /**
     * 查询模板
     */

    // 查询未被删除的记录列表-分页--排序逻辑按照defaultSortMode方法逻辑
    public PageSelectResWrapper<Res> selectByPageMustNotDel(Req requestDto);

    // 查询未被删除的记录列表-分页--排序逻辑按照传参逻辑
    public PageSelectResWrapper<Res> selectByPageMustNotDel(Req requestDto, String sortField, String sortType);

    // 查询未被删除的记录列表--排序逻辑按照defaultSortMode方法逻辑
    public List<Res> selectListMustNotDel(Req requestDto);

    // 查询未被删除的记录列表--排序逻辑按照传参逻辑
    public List<Res> selectListMustNotDelWithSortField(Req requestDto, String sortField, String sortType);

    // 查询未被删除的记录列表--查询指定字段
    public List<Res> selectListMustNotDelWithPoFields(Req requestDto, String... selectFields);

    // 查询列表返回标准Options类型
    public List<Map> selectOptionList(Req requestDto);

    // 根据id查询未被删除的记录
    public Res selectByIdMustNotDel(Serializable id);

    // 根据id查询未被删除并生效的记录
    public Res selectByIdMustNotDelAndIsValid(Serializable id);

    // 查询单条记录，必须非多个且未被删除，否则异常
    public Res selectOneMustNotDelAndMultiple(Req dto);

    /**
     * 删除模板
     */

    // 删除模板-必须存在或未被删除否则异常
    public Boolean deleteMustExist(Req requestDto);

    // 删除模板
    public Boolean deleteTemplate(Req requestDto);

    // 根据id删除但必须存在，否则异常
    public Boolean deleteByIdMustExist(Serializable id);

    /**
     * 生效记录与失效记录
     */

    // 根据id设置生效
    public Boolean setValidBeTrueById(Serializable id);

    // 根据id设置失效
    public Boolean setValidBeFalseById(Serializable id);

    /**
     * default开头的方法为默认实现，可以根据业务需求重写
     */

    // 默认Dto查询逻辑为主键匹配-用于Req传参情况
    public P defaultSelectByDto(Req obj);

    // 根据id查询-用于id传参的情况
    public P defaultSelectById(Serializable id);

    // 默认复制属性逻辑，requestDto转Po， String类型为null则填充""
    public void defaultReqConvertPo(Req requestDto, P p);

    // 默认复制属性逻辑，Po转responseDto
    public void defaultPoConvertRes(P po,Res responseDto);

    // 默认的查询条件生成器
    public QueryWrapper<P> defaultGetQueryWrapper(Req requestDto);
    // 默认的更新条件生成器
    public UpdateWrapper<P> defaultGetUpdateWrapper(P po);

    // 默认排序逻辑，按照update_date倒叙排序
    public void defaultSortMode(Page<P> page);


}
