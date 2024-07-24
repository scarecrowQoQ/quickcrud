package com.tml.quickcrud.template.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageSelectResWrapper<Res> {
    private Long total;

    private List<Res> list;
}

