package com.yupi.aitravelplanner.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求包装类
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 要删除的记录 id
     */
    private Long id;
}
