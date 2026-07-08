package com.fu.fuaicode.ai;

import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * AI代码生成类型路由服务
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户提示词获得服务
     * 结构化输出枚举类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userMessage);
}
