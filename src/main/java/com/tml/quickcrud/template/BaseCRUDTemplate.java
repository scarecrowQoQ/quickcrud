package com.tml.quickcrud.template;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tml.quickcrud.template.domain.FormRule;
import com.tml.quickcrud.template.domain.PageSelectResWrapper;
import com.tml.quickcrud.template.domain.PropertyInfo;
import com.tml.quickcrud.template.domain.ResContent;
import com.tml.quickcrud.template.service.BaseServiceTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.Serializable;
import java.util.List;

// 基础增删改查接口
public interface BaseCRUDTemplate <Req,Impl extends BaseServiceTemplate>{

    public BaseMapper getBaseMapper();

    /**
     * 以下四个为前端字段视图配置，由业务模板父类已实现
     */
    public List<PropertyInfo> getSelectConfig();

    public List<PropertyInfo> getResultViewConfig();

    public List<PropertyInfo> getInsertConfig();

    public List<PropertyInfo> getUpdateConfig();

    public FormRule getFormRule();

    /**
     * 下面为模板默认实现
     * @param id
     * @return
     */
    public default ResContent selectById(Serializable id){
        return ResContent.success(((Impl)this).selectByIdMustNotDel(id));
    }

    public default ResContent selectByPage(Req req){
        PageSelectResWrapper pageSelectResWrapper = ((Impl)this).selectByPageMustNotDel(req);
        return ResContent.success(pageSelectResWrapper);
    }

    public default ResContent selectList(Req req){
        return ResContent.success(((Impl)this).selectListMustNotDel(req));
    }

    public default ResContent selectOption(Req req){
        return ResContent.success(((Impl)this).selectOptionList(req));
    }

    public default ResContent selectOne(Req req){
        return ResContent.success(((Impl)this).selectOneMustNotDelAndMultiple(req));
    }

    public default ResContent insert(Req req){
        return ResContent.success(((Impl)this).insertMustNotExist(req));
    }

    public default ResContent importFromExcel(MultipartFile file){return ResContent.success(((Impl)this).insertFromExcel(file));}

    public default ResContent update(Req req){
        return ResContent.success(((Impl)this).updateTemplate(req));
    }

    public default ResContent deleteById(Serializable id){
        return ResContent.success(((Impl)this).deleteByIdMustExist(id));
    }

    public default ResContent enable(Serializable id){
        return ResContent.success(((Impl)this).setValidBeTrueById(id));
    }

    public default ResContent disable(Serializable id){
        return ResContent.success(((Impl)this).setValidBeFalseById(id));
    }
}

