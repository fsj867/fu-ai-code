package com.fu.fuaicode.core.saver;

import com.fu.fuaicode.ai.model.HtmlCodeResult;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存模板
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFile(String dirPath, HtmlCodeResult result) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
    }
    @Override
    protected void validateInput(HtmlCodeResult result){
        super.validateInput(result);
        if (result.getHtmlCode() == null || result.getHtmlCode().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"HTML保存代码为空");
        }
    }
}


