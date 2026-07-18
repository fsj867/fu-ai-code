package com.fu.fuaicode.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildTask implements Serializable {

    private static final long serialVersionUID = 1L;
    // 应用ID
    private Long appId;
    // 工作目录
    private String workingDir;
    // 生成的代码目录
    private String generatedCodeDir;
}