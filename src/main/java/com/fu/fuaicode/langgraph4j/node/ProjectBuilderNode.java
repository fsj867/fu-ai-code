package com.fu.fuaicode.langgraph4j.node;

import com.fu.fuaicode.core.builder.VueProjectBuilder;
import com.fu.fuaicode.langgraph4j.SpringContextUtil;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;
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
                    
                    // 检查是否已有构建结果（dist 目录存在且是目录）
                    File distDir = new File(generatedCodeDir, "dist");
                    if (distDir.exists() && distDir.isDirectory()) {
                        log.info("检测到已存在 dist 目录，跳过构建: {}", distDir.getAbsolutePath());
                        buildResultDir = distDir.getAbsolutePath();
                    } else {
                        // 检查是否正在构建中，避免并发重复构建
                        VueProjectBuilder.BuildStatus existingStatus = vueBuilder.getBuildStatus(generatedCodeDir);
                        if (existingStatus != null && 
                            existingStatus.getPhase() != VueProjectBuilder.BuildStatus.BuildPhase.COMPLETED &&
                            existingStatus.getPhase() != VueProjectBuilder.BuildStatus.BuildPhase.FAILED) {
                            log.warn("检测到该目录已有构建在进行中，等待完成: {}", generatedCodeDir);
                        }
                        
                        boolean buildSuccess = vueBuilder.buildVueProject(generatedCodeDir);
                        if (buildSuccess) {
                            buildResultDir = distDir.getAbsolutePath();
                            log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                        } else {
                            String errorMsg = "Vue 项目构建失败";
                            VueProjectBuilder.BuildStatus status = vueBuilder.getBuildStatus(generatedCodeDir);
                            if (status != null && status.getErrorMessage() != null) {
                                errorMsg = status.getErrorMessage();
                            }
                            log.error(errorMsg);
                            context.setErrorMessage(errorMsg);
                            buildResultDir = generatedCodeDir;
                        }
                    }
                } catch (Exception e) {
                    log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                    context.setErrorMessage("构建异常: " + e.getMessage());
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
