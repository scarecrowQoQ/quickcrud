package com.tml.quickcrud.template.domain;

/**
 * 基本字段信息抽象类
 * 作用：模板在处理一些逻辑时涉及到例如：是否删除、是否有效等需要使用字段来进行判断，
 * 但是不同表以及不同类的字段名称不一样比如time与date，甚至有的表没有is_del字段而是只用is_valid字段，所以需要抽象出来一个基本字段信息类
 * 模板依赖于抽象逻辑而不再是具体的字段名称，在不同的类中实现该抽象类的方法即可
 */
public abstract class BaseRecordInfo<PO> {
    // 初始化操作记录
    public abstract void initializeBaseRecordInfo();
    // 更新操作记录
    public abstract void updateBaseRecordInfo();
    // 判断是否删除
    public abstract Boolean isDel();
    // 设置删除状态
    public abstract void setDel(boolean isDel);
    // 获取删除字段
    public abstract String initDelField();
    // 判断是否有效
    public abstract Boolean isValid();
    // 设置为有效
    public abstract void setValid(boolean isValid);
    // 获取有效字段
    public abstract String initValidField();
    // 默认的排序字段
    public abstract String initSortModeField();

    public abstract PO PropertyViewRegisterImpl();

}
