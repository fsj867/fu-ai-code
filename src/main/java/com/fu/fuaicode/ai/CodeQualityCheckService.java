package com.fu.fuaicode.ai;

import com.fu.fuaicode.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CodeQualityCheckService {

    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
     QualityResult checkCodeQuality(@UserMessage String codeContent);
}
