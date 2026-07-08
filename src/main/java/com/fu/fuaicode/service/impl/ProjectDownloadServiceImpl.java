package com.fu.fuaicode.service.impl;


import cn.hutool.core.util.ZipUtil;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.exception.ThrowUtils;
import com.fu.fuaicode.service.ProjectDownloadService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 检查路径是否允许包含在压缩包中
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);
        // 检查路径中的每一部分
        for (Path part : relativePath) {
            String partName = part.toString();
            // 检查是否在忽略名称列表中
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // 检查文件扩展名
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public void downloadProjcetAsZip(String projectPath,String projectName, HttpServletResponse response){
        ThrowUtils.throwIf(projectPath == null, ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        ThrowUtils.throwIf(projectName == null, ErrorCode.PARAMS_ERROR, "项目名称不能为空");
        File projectRoot = new File(projectPath);
        ThrowUtils.throwIf(!projectRoot.exists(), ErrorCode.PARAMS_ERROR, "项目路径不存在");
        ThrowUtils.throwIf(!projectRoot.isDirectory(), ErrorCode.PARAMS_ERROR, "项目路径不是一个目录");
        log.info("开始压缩项目：{}", projectPath);
        // 设置响应
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + projectName + ".zip\"");
        // 过滤器
        FileFilter filter = file -> isPathAllowed(projectRoot.toPath(), file.toPath());
        try{
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8,false,filter,projectRoot);
            log.info("项目压缩完成：{}", projectPath);
        }catch (Exception e){
            log.error("项目压缩失败：{}", projectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目下载失败");
        }
    }
}
