package com.fu.fuaicode.core.parser;

import com.fu.fuaicode.model.enums.CodeGenTypeEnum;

/**
 * 代码解析执行器
 */
public class CodeParserExecutor {
    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 执行代码解析
     *
     */
    public static Object execute(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {
       return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
           default -> {
               throw new IllegalArgumentException("不支持的代码类型" + codeGenTypeEnum);
           }
        };
    }
}
