package com.fu.fuaicode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MessageTypeEnum {

    USER("用户消息", "user"),
    AI("AI消息", "ai"),
    ERROR("错误消息", "error");

    private final String description;
    private final String value;

    MessageTypeEnum(String description, String value) {
        this.description = description;
        this.value = value;
    }

    public static MessageTypeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MessageTypeEnum messageTypeEnum : MessageTypeEnum.values()) {
            if (messageTypeEnum.getValue().equals(value)) {
                return messageTypeEnum;
            }
        }
        return null;
    }
}
