package com.fu.fuaicode.core.saver;

import com.fu.fuaicode.ai.model.MultiFileCodeResult;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult>{

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFile(String dirPath, MultiFileCodeResult result) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
    }
    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        if (result.getHtmlCode() == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"HTML代码不能为空");
        }
    }
}


