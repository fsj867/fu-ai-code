package com.fu.fuaicode.core;

import com.fu.fuaicode.ai.AiCodeGeneratorService;
import com.fu.fuaicode.ai.AiCodeGeneratorServiceFactory;
import com.fu.fuaicode.ai.model.HtmlCodeResult;
import com.fu.fuaicode.ai.model.MultiFileCodeResult;
import com.fu.fuaicode.constant.AppConstant;
import com.fu.fuaicode.core.builder.VueProjectBuilder;
import com.fu.fuaicode.core.parser.CodeParserExecutor;
import com.fu.fuaicode.core.saver.CodeFileSaverExecutor;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 代码生成器门面类
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    
    /**
     * 统一入口：根据类型生成代码
     * @param message 用户消息
     * @param codeGenTypeEnum 代码生成类型
     */
    public File generateAndSaveCode(String message, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.aiCodeGeneratorService(appId,codeGenTypeEnum);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"代码生成类型不能为空");
        }
        String value = codeGenTypeEnum.getValue();
        return switch (value){
            case "html" -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(message);
                yield CodeFileSaverExecutor.saveFile(htmlCodeResult, codeGenTypeEnum, appId);
            }
            case "multi_file" -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(message);
                yield CodeFileSaverExecutor.saveFile(multiFileCodeResult, codeGenTypeEnum, appId);
            }
            default -> {
                String Error_Message = "不支持的代码生成类型" + value;
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,Error_Message);
            }
        };
    }


    /**
     * 统一入口：生成与保存代码（流式）
     * @param message 用户消息
     * @param codeGenTypeEnum 代码生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String message, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.aiCodeGeneratorService(appId,codeGenTypeEnum);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"代码生成类型不能为空");
        }
       return switch (codeGenTypeEnum){
           case HTML -> {
               Flux<String> htmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(message);
               yield processStream(htmlCodeStream, codeGenTypeEnum, appId);
           }
           case MULTI_FILE -> {
               Flux<String> multiFileCodeStream = aiCodeGeneratorService.generateMultiFileCodeStream(message);
               yield processStream(multiFileCodeStream, codeGenTypeEnum, appId);
           }
           case VUE_PROJECT -> {
               Flux<String> vueProjectCodeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, message);
               yield processStream(vueProjectCodeStream, codeGenTypeEnum, appId);
           }
           default ->{
               throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型");
           }
       };

    }
    public Flux<String> processStream(Flux<String> stream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return stream.doOnNext(
                chunk ->{
                    codeBuilder.append(chunk);
                })
                .doOnComplete(() ->{
                    // VUE_PROJECT类型通过工具调用直接写入文件，无需再次解析保存
                    if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
                        log.info("VUE_PROJECT类型已通过工具调用保存文件");
                        log.info("开始构建Vue项目: {}", appId);
                        String workingDir = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildVueProjectAsync(workingDir);
                        return;
                    }
                    try {
                        String code = codeBuilder.toString();
                        Object execute = CodeParserExecutor.execute(code, codeGenTypeEnum);
                        File file = CodeFileSaverExecutor.saveFile(execute, codeGenTypeEnum, appId);
                        log.info("保存成功：{}", file.getAbsolutePath());
                    }catch (Exception e){
                        log.error("保存失败：{}", e.getMessage());
                    }

                });
    }
}
