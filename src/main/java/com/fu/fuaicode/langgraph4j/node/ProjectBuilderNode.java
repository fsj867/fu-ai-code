package com.fu.fuaicode.langgraph4j.node;

import com.fu.fuaicode.core.builder.VueProjectBuilder;
import com.fu.fuaicode.langgraph4j.SpringContextUtil;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;
import com.fu.fuaicode.mq.BuildTask;
import com.fu.fuaicode.mq.BuildTaskProducer;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");

            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeGenTypeEnum generationType = context.getGenerationType();
            String buildResultDir;

            if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
                try {
                    VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                    
                    File distDir = new File(generatedCodeDir, "dist");
                    if (distDir.exists() && distDir.isDirectory()) {
                        log.info("检测到已存在 dist 目录，删除以触发重新构建: {}", distDir.getAbsolutePath());
                        cn.hutool.core.io.FileUtil.del(distDir);
                    }
                    
                    VueProjectBuilder.BuildStatus existingStatus = vueBuilder.getBuildStatus(generatedCodeDir);
                    if (existingStatus != null && 
                        existingStatus.getPhase() != VueProjectBuilder.BuildStatus.BuildPhase.COMPLETED &&
                        existingStatus.getPhase() != VueProjectBuilder.BuildStatus.BuildPhase.FAILED) {
                        log.warn("检测到该目录已有构建在进行中，跳过重复发送: {}", generatedCodeDir);
                        buildResultDir = generatedCodeDir;
                    } else {
                        vueBuilder.resetBuildStatus(generatedCodeDir);
                        BuildTaskProducer producer = SpringContextUtil.getBean(BuildTaskProducer.class);
                        BuildTask task = BuildTask.builder()
                                .appId(context.getAppId())
                                .workingDir(generatedCodeDir)
                                .generatedCodeDir(generatedCodeDir)
                                .build();
                        producer.sendBuildTask(task);
                        log.info("已发送异步构建任务: appId={}, workingDir={}", context.getAppId(), generatedCodeDir);
                        buildResultDir = generatedCodeDir;
                    }
                } catch (Exception e) {
                    log.error("发送构建任务异常: {}", e.getMessage(), e);
                    context.setErrorMessage("发送构建任务异常: " + e.getMessage());
                    buildResultDir = generatedCodeDir;
                }
            } else {
                buildResultDir = generatedCodeDir;
                log.info("非 Vue 项目类型，跳过构建: {}", generationType);
            }

            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建节点完成，最终目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
