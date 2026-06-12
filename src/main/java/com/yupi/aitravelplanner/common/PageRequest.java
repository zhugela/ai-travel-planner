package com.yupi.aitravelplanner.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求包装类
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 每页大小
     */
    private int pageSize = 10;
}
