package com.fu.fuaicode.langgraph4j.state;
import lombok.Data;
@Data
public class WorkflowEvent {

    public static final String TYPE_STEP = "step";
    public static final String TYPE_TOKEN = "token";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_COMPLETE = "complete";

    private String eventType;
    private String stepName;
    private WorkflowContext context;
    private String content;
}
