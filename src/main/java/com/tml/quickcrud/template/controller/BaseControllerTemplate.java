package com.tml.quickcrud.template.controller;

import com.tml.quickcrud.template.BaseCRUDTemplate;
import com.tml.quickcrud.template.domain.ResContent;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import java.io.Serializable;

/**
 * 基础Controller接口模板，继承此模板来对控制层进行增强。封装，继承，多态！
 * @param <S> 为服务抽象接口，必须继承BaseCRUDTemplate进行增强，并指定Req类型
 * @param <Req> 入参对象类型
 */
@RequiredArgsConstructor
public class BaseControllerTemplate<S extends BaseCRUDTemplate<Req, ?>, Req>{

    protected final S service;

    public S getService(){
        return service;
    }

    @PostMapping("/selectByPage")
    public ResContent selectByPage(@RequestBody Req dto) {
        return service.selectByPage(dto);
    }

    @PostMapping("/selectById/{id}")
    public ResContent selectById(@PathVariable("id") Serializable id) {
        return service.selectById(id);
    }

    @PostMapping("selectAll")
    public ResContent selectAll(@RequestBody Req dto) {
        return service.selectList(dto);
    }

    @PostMapping("selectOption")
    public ResContent selectOption(@RequestBody Req dto) {
        return service.selectOption(dto);
    }

    @PostMapping("selectOne")
    public ResContent selectOne(@RequestBody Req dto) {
        return service.selectOne(dto);
    }

    @PostMapping("/save")
    public ResContent insert(@RequestBody @Validated Req dto) {
        return service.insert(dto);
    }

    @PostMapping("/update")
    public ResContent update(@RequestBody @Validated Req dto) {
        return service.update(dto);
    }

    @PostMapping("/deleteById/{id}")
    public ResContent deleteById(@PathVariable("id") Serializable id) {
        return service.deleteById(id);
    }

    @PostMapping("/enable/{id}")
    public ResContent enable(@PathVariable("id") Serializable id) {
        return service.enable(id);
    }

    @PostMapping("/disable/{id}")
    public ResContent disable(@PathVariable("id") Serializable id) {
        return service.disable(id);
    }

    @PostMapping("/import")
    public ResContent disable(MultipartFile file){
        return service.importFromExcel(file);
    }

}
