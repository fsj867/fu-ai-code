package com.fu.fuaicode.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            // 执行命令
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
    private String getCommand(String baseCommand) {
        if (isWindows()){
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }
    /**
     * 执行npm install
     * @param workingDir 工作目录
     * @return 是否执行成功
     */
    public boolean npmInstall(File workingDir) {
        log.info("开始执行npm install");
        String command = String.format("%s install", getCommand("npm"));
        return executeCommand(workingDir, command, 300); // 设置超时为5分钟

    }
    /**
     * 执行npm run build
     * @param workingDir 工作目录
     * @return 是否执行成功
     */
    public boolean npmRunBuild(File workingDir) {
        log.info("开始执行npm run build");
        String command = String.format("%s run build", getCommand("npm"));
        return executeCommand(workingDir, command, 300); // 设置超时为5分钟
    }
    /**
     * 构造vue 项目
     * @Param workingDir 工作目录
     * @return 是否执行成功
     */
    public boolean buildVueProject(String workingDir) {
        File projectDir = new File(workingDir);
        if (!projectDir.exists()||!projectDir.isDirectory()) {
            log.error("工作目录不存在或不是一个目录: {}", workingDir);
            return false;
        }
        // 检查package.json文件是否存在
        File packageJson = new File(projectDir, "package.json"); // package.json文件路径
        if (!packageJson.exists()) {
            log.error("package.json文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建Vue项目: {}", workingDir);
        // 执行npm install
        if (!npmInstall(projectDir)) {
            log.error("npm install 失败");
            return false;
        }
        // 执行npm run build
        if (!npmRunBuild(projectDir)) {
            log.error("npm run build 失败");
            return false;
        }
        File distDir = new File(projectDir, "dist"); // dist目录路径
        if (!distDir.exists()||!distDir.isDirectory()) {
            log.error("dist目录不存在: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue项目构建成功: {}", distDir.getAbsolutePath());
        return true;
    }
    /**
     * 异步构建Vue项目
     * @Param workingDir 工作目录
     * @return 是否执行成功
     */
    public void buildVueProjectAsync(String workingDir) {

        Thread.ofVirtual().name("vue-project-builder" +System.currentTimeMillis()).start(() -> {
            try {
                buildVueProject(workingDir);
            }catch (Exception e){
                log.error("异步构建Vue项目失败: {}", e.getMessage());
            }
        });
    }

}
