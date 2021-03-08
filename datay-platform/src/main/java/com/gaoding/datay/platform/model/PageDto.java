package com.gaoding.datay.platform.model;

import java.util.List;

import lombok.Data;

@Data
public class PageDto<T> {
    

    public List<T> data;

    private Long total;


}
