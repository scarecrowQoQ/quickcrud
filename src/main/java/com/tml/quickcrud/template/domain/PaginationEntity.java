package com.tml.quickcrud.template.domain;

import com.baomidou.mybatisplus.annotation.TableField;

public class PaginationEntity{
    @TableField(exist = false)
    private Integer pageNum;
    @TableField(exist = false)
    private Integer pageSize;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer calcStartNum() {
        return pageNum <= 1 ? 0 : (pageNum - 1) * pageSize;
    }
}
