package com.fu.fuaicode.core.parser;

import com.fu.fuaicode.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 代码解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    @Override
    public HtmlCodeResult parseCode(String code){
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractCodeByPattern(code, HTML_CODE_PATTERN);
        if(htmlCode != null && !htmlCode.trim().isEmpty()) {
            htmlCodeResult.setHtmlCode(htmlCode.trim());
        }else {
            //如果没有找到代码块，将整个内容作为HTML
            htmlCodeResult.setHtmlCode(code.trim());
        }
        return htmlCodeResult;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
