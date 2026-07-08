package com.fu.fuaicode.model.vo;

import com.fu.fuaicode.model.entity.App;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用视图对象
 *
 * @author fsj
 */
@Data
public class AppVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 从实体转换为视图对象
     */
    public static AppVO fromEntity(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        appVO.setId(app.getId());
        appVO.setAppName(app.getAppName());
        appVO.setCover(app.getCover());
        appVO.setInitPrompt(app.getInitPrompt());
        appVO.setCodeGenType(app.getCodeGenType());
        appVO.setDeployKey(app.getDeployKey());
        appVO.setDeployedTime(app.getDeployedTime());
        appVO.setPriority(app.getPriority());
        appVO.setUserId(app.getUserId());
        appVO.setCreateTime(app.getCreateTime());
        appVO.setUpdateTime(app.getUpdateTime());
        return appVO;
    }
}