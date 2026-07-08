package com.fu.fuaicode.ai.tools;

import com.fu.fuaicode.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 *  支持AI通过工具调用，将结果写入文件
 */
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(@P("文件相对路径") String relativePath,
                            @P("文件内容") String content,
                            @ToolMemoryId Long appId) {
        log.info("写入文件到指定路径: {}，内容: {}", relativePath, content);
        try {
            Path path = Paths.get(relativePath);
            if (!path.isAbsolute()){
                // 相对路径处理成绝对路径
                String project_Name = "vue_project_" + appId;
                Path project_Path= Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, project_Name);
                path = project_Path.resolve(relativePath); // 相对路径处理成绝对路径
            }
            //父目录不存在创建
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)){
                Files.createDirectories(parent);
            }
            //写入文件
            Files.writeString(path, content,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "文件写入成功："+relativePath;
        } catch (IOException e) {
            log.error("文件写入失败", e);
            return "文件写入失败："+e.getMessage();
        }
    }
}
