package com.fu.fuaicode.langgraph4j.node;

import com.fu.fuaicode.langgraph4j.SpringContextUtil;
import com.fu.fuaicode.langgraph4j.state.ImageResource;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;
import com.fu.fuaicode.langgraph4j.state.WorkflowEvent;
import com.fu.fuaicode.model.enums.ImageCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Arrays;
import java.util.List;
import com.fu.fuaicode.ai.ImageCollectionService;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 图片收集");

            WorkflowEvent startEvent = new WorkflowEvent();
            startEvent.setEventType(WorkflowEvent.TYPE_STEP);
            startEvent.setStepName("图片收集");
            context.emitEvent(startEvent);

            String originalPrompt = context.getOriginalPrompt();
            String imageStr = "";
            try {
                // 调用图片收集服务
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                imageStr = imageCollectionService.generateImageCollection(originalPrompt);
            }catch (Exception e){
                log.error("图片收集服务调用失败: {}", e.getMessage());
            }

            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageListStr(imageStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
