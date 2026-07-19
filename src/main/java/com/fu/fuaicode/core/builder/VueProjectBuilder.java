package com.fu.fuaicode.core.builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {

    private static final int DEFAULT_TIMEOUT_SECONDS = 600; // 默认超时时间（秒）

    private static final String NPM_REGISTRY = "https://registry.npmmirror.com"; // NPM 镜像

    private final Map<String, BuildStatus> buildStatusMap = new ConcurrentHashMap<>(); // 构建状态缓射

    @Data
    @AllArgsConstructor
    public static class BuildStatus {
        private String workingDir;
        private BuildPhase phase;
        private boolean success;
        private String errorMessage;
        private long startTime;
        private long endTime;

        public enum BuildPhase {
            PENDING,
            NPM_INSTALL,
            NPM_BUILD,
            COMPLETED,
            FAILED
        }
    }

    public BuildStatus getBuildStatus(String workingDir) {
        return buildStatusMap.get(workingDir);
    }

    public void resetBuildStatus(String workingDir) {
        buildStatusMap.remove(workingDir);
    }

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

            List<String> commandList = new ArrayList<>();
            if (isWindows()) {
                commandList.add("cmd.exe");
                commandList.add("/c");
            }
            for (String part : command.split("\\s+")) {
                if (!part.isEmpty()) {
                    commandList.add(part);
                }
            }

            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();
            Thread outputReaderThread = Thread.startVirtualThread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                        log.debug("[build-output] {}", line);
                    }
                } catch (Exception e) {
                    log.warn("读取命令输出异常: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程: {}", timeoutSeconds, command);
                log.error("最后输出内容:\n{}", outputBuilder);
                process.destroyForcibly();
                try {
                    outputReaderThread.join(5000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }

            try {
                outputReaderThread.join(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}, 命令: {}", exitCode, command);
                log.error("错误输出:\n{}", outputBuilder);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage(), e);
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * 执行npm install
     * @param workingDir 工作目录
     * @return 是否执行成功
     */
    public boolean npmInstall(File workingDir) {
        log.info("开始执行npm install，工作目录: {}, 镜像源: {}", workingDir.getAbsolutePath(), NPM_REGISTRY);
        String command = String.format("%s install --registry=%s", getCommand("npm"), NPM_REGISTRY);
        return executeCommand(workingDir, command, DEFAULT_TIMEOUT_SECONDS);

    }

    /**
     * 执行npm run build
     * @param workingDir 工作目录
     * @return 是否执行成功
     */
    public boolean npmRunBuild(File workingDir) {
        log.info("开始执行npm run build，工作目录: {}", workingDir.getAbsolutePath());
        String command = String.format("%s run build", getCommand("npm"));
        return executeCommand(workingDir, command, DEFAULT_TIMEOUT_SECONDS);
    }

    private String getCommand(String baseCommand) {
        return baseCommand;
    }
    /**
     * 构造vue 项目
     * @Param workingDir 工作目录
     * @return 是否执行成功
     */
    public boolean buildVueProject(String workingDir) {
        BuildStatus existingStatus = buildStatusMap.get(workingDir);
        if (existingStatus != null &&
            existingStatus.getPhase() != BuildStatus.BuildPhase.COMPLETED &&
            existingStatus.getPhase() != BuildStatus.BuildPhase.FAILED) {
            log.warn("该目录已有构建在进行中，跳过重复构建: {}", workingDir);
            return false;
        }

        BuildStatus status = new BuildStatus(
                workingDir,
                BuildStatus.BuildPhase.PENDING,
                false,
                null,
                System.currentTimeMillis(),
                0
        );
        buildStatusMap.put(workingDir, status);

        File projectDir = new File(workingDir);
        if (!projectDir.exists()||!projectDir.isDirectory()) {
            String errorMsg = "工作目录不存在或不是一个目录: " + workingDir;
            log.error(errorMsg);
            updateBuildStatus(workingDir, BuildStatus.BuildPhase.FAILED, false, errorMsg);
            return false;
        }
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            String errorMsg = "package.json文件不存在: " + packageJson.getAbsolutePath();
            log.error(errorMsg);
            updateBuildStatus(workingDir, BuildStatus.BuildPhase.FAILED, false, errorMsg);
            return false;
        }
        log.info("开始构建Vue项目: {}", workingDir);

        File distDir = new File(projectDir, "dist");
        if (distDir.exists()) {
            deleteDirectory(distDir);
            log.info("已删除旧的 dist 目录: {}", distDir.getAbsolutePath());
        }

        File nodeModulesDir = new File(projectDir, "node_modules");
        if (nodeModulesDir.exists() && nodeModulesDir.isDirectory()) {
            log.info("检测到已存在 node_modules，跳过 npm install: {}", nodeModulesDir.getAbsolutePath());
        } else {
            updateBuildStatus(workingDir, BuildStatus.BuildPhase.NPM_INSTALL, false, null);
            if (!npmInstall(projectDir)) {
                String errorMsg = "npm install 失败，请查看日志获取详细错误信息";
                log.error(errorMsg);
                updateBuildStatus(workingDir, BuildStatus.BuildPhase.FAILED, false, errorMsg);
                return false;
            }
        }

        updateBuildStatus(workingDir, BuildStatus.BuildPhase.NPM_BUILD, false, null);
        if (!npmRunBuild(projectDir)) {
            String errorMsg = "npm run build 失败，请查看日志获取详细错误信息";
            log.error(errorMsg);
            updateBuildStatus(workingDir, BuildStatus.BuildPhase.FAILED, false, errorMsg);
            return false;
        }

        if (!distDir.exists()||!distDir.isDirectory()) {
            String errorMsg = "dist目录不存在: " + distDir.getAbsolutePath();
            log.error(errorMsg);
            updateBuildStatus(workingDir, BuildStatus.BuildPhase.FAILED, false, errorMsg);
            return false;
        }

        updateBuildStatus(workingDir, BuildStatus.BuildPhase.COMPLETED, true, null);
        log.info("Vue项目构建成功: {}", distDir.getAbsolutePath());
        return true;
    }

    private void updateBuildStatus(String workingDir, BuildStatus.BuildPhase phase, boolean success, String errorMessage) {
        BuildStatus status = buildStatusMap.get(workingDir);
        if (status != null) {
            status.setPhase(phase);
            status.setSuccess(success);
            status.setErrorMessage(errorMessage);
            if (phase == BuildStatus.BuildPhase.COMPLETED || phase == BuildStatus.BuildPhase.FAILED) {
                status.setEndTime(System.currentTimeMillis());
            }
        }
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * 异步构建Vue项目
     * @Param workingDir 工作目录
     */
    public void buildVueProjectAsync(String workingDir) {
        Thread.ofVirtual().name("vue-project-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildVueProject(workingDir);
            } catch (Exception e) {
                log.error("异步构建Vue项目失败: {}", e.getMessage(), e);
                updateBuildStatus(workingDir, BuildStatus.BuildPhase.FAILED, false, e.getMessage());
            }
        });
    }

}
