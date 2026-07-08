package com.fu.fuaicode.langgraph4j.node;

import com.fu.fuaicode.ai.AiCodeGenTypeRoutingService;
import com.fu.fuaicode.langgraph4j.SpringContextUtil;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;

import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;


import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");
            CodeGenTypeEnum generationType;
            try {
                AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                //根据原始提示进行智能路由
                generationType = aiCodeGenTypeRoutingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("智能路由完成，选择类型: {}", generationType.getValue());
            }catch (Exception e){
                log.error("智能路由服务调用失败: {},默认使用HTML", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }


            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            log.info("路由决策完成，选择类型: {}", generationType.getValue());
            return WorkflowContext.saveContext(context);
        });
    }
}
