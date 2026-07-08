package com.fu.fuaicode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fu.fuaicode.constant.AppConstant;
import com.fu.fuaicode.constant.UserConstant;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存模板
 */
public abstract class CodeFileSaverTemplate<T> {
    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法
     * @Param result 代码结果
     * @Param appId 应用id
     * @return 保存后的文件目录
     */
    public final File saveCode(T result, Long appId) {
        //1. 验证输入
        validateInput(result);
        //2. 构建唯一目录
        String dirPath = buildUniqueDir(appId);
        //3. 保存文件
        saveFile(dirPath, result);
        //4. 返回文件目录
        return new File(dirPath);
    }

    protected void validateInput(T result){
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"保存代码为空");
        }
    }
    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     */
    protected final String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"应用id为空");
        }
        String codeType = getCodeType().getValue().toString();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }
    /**
     * 写入单个文件
     */
    protected static void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
    /**
     * 获得代码类型（子类实现）
     *
     */
    protected abstract  CodeGenTypeEnum getCodeType();
    /**
     * 保存文件（子类实现）
     */
    protected abstract void saveFile(String dirPath, T result);
}
