package com.fu.fuaicode.model.enums;


import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {
    HTML("原生 Html 模式", "html"),
    MULTI_FILE("原生多文件模式", "multi_file"),
    VUE_PROJECT("Vue 工程模式", "vue_project");

    private String description;
    private String value;

    CodeGenTypeEnum(String description, String value) {
        this.description = description;
        this.value = value;
    }

    public static CodeGenTypeEnum getByValue(String value) {
        if(ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CodeGenTypeEnum codeGenTypeEnum : CodeGenTypeEnum.values()) {
            if (codeGenTypeEnum.getValue().equals(value)) {
                return codeGenTypeEnum;
            }
        }
        return null;
    }
}
