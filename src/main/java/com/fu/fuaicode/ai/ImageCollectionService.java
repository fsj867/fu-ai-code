package com.fu.fuaicode.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ImageCollectionService {

    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.txt")
    String generateImageCollection(@UserMessage String userPrompt);
}
