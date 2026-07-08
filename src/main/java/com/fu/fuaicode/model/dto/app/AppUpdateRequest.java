package com.fu.fuaicode.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新应用请求（仅支持修改应用名称）
 *
 * @author fsj
 */
@Data
public class AppUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;
}