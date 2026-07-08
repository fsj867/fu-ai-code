package com.fu.fuaicode.core.saver;

import com.fu.fuaicode.ai.model.HtmlCodeResult;
import com.fu.fuaicode.ai.model.MultiFileCodeResult;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;

import java.io.File;

import static com.fu.fuaicode.model.enums.CodeGenTypeEnum.HTML;
import static com.fu.fuaicode.model.enums.CodeGenTypeEnum.MULTI_FILE;

/**
 * 代码文件保存执行器
 */
public class CodeFileSaverExecutor {
    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaverTemplate = new MultiFileCodeFileSaverTemplate();

    public static File saveFile(Object codeResult, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaverTemplate.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new RuntimeException("不支持的代码类型" + codeGenTypeEnum);
        };
    }
}
