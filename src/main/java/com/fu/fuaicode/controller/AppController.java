package com.fu.fuaicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import cn.hutool.json.JSONUtil;
import com.fu.fuaicode.annotation.AuthCheck;
import com.fu.fuaicode.common.BaseResponse;
import com.fu.fuaicode.common.DeleteRequest;
import com.fu.fuaicode.common.ResultUtils;
import com.fu.fuaicode.constant.AppConstant;
import com.fu.fuaicode.constant.UserConstant;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.exception.ThrowUtils;
import com.fu.fuaicode.langgraph4j.CodeGenWorkflow;
import com.fu.fuaicode.langgraph4j.state.WorkflowEvent;
import com.fu.fuaicode.model.dto.app.*;
import com.fu.fuaicode.model.entity.App;
import com.fu.fuaicode.model.entity.User;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import com.fu.fuaicode.model.vo.AppVO;
import com.fu.fuaicode.ratelimit.annonation.RateLimit;
import com.fu.fuaicode.ratelimit.enums.RateLimitType;
import com.fu.fuaicode.service.AppService;
import com.fu.fuaicode.service.ProjectDownloadService;
import com.fu.fuaicode.service.UserService;

import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.reactivestreams.Publisher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;


/**
 * 应用 控制层。
 *
 * @AUTHOR FSJ
 */
@Slf4j
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private CodeGenWorkflow codeGenWorkflow;
    
    /**
     * 用户创建应用
     *
     * @param appAddRequest 创建请求
     * @param request       请求对象
     * @return 应用id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser.getId());
        return ResultUtils.success(appId);
    }

    /**
     * 用户根据id修改自己的应用（目前只支持修改应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求对象
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        
        boolean result = appService.updateApp(appUpdateRequest, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 用户根据id删除自己的应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        
        boolean result = appService.deleteApp(deleteRequest.getId(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 用户根据id查看应用详情
     *
     * @param id      应用id
     * @param request 请求对象
     * @return 应用详情
     */
    @GetMapping("/get")
    public BaseResponse<App> getAppDetail(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        
        App app = appService.getAppDetail(id, loginUser.getId());
        return ResultUtils.success(app);
    }

    /**
     * 用户分页查询自己的应用列表（支持根据名称查询，每页最多20个）
     *
     * @param appName  应用名称（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param request  请求对象
     * @return 应用列表
     */
    @GetMapping("/list/my")
    public BaseResponse<Map<String, Object>> listMyApps(
            @RequestParam(required = false) String appName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        
        User loginUser = userService.getLoginUser(request);
        
        List<AppVO> appVOList = appService.listUserApps(loginUser.getId(), appName, pageNum, pageSize);
        long total = appService.countUserApps(loginUser.getId(), appName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", appVOList);
        result.put("total", total);
        
        return ResultUtils.success(result);
    }

    /**
     * 用户分页查询精选的应用列表（支持根据名称查询，每页最多20个）
     *
     * @param appName  应用名称（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 应用列表
     */
    @GetMapping("/list/featured")
    public BaseResponse<Map<String, Object>> listFeaturedApps(
            @RequestParam(required = false) String appName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        List<AppVO> appVOList = appService.listFeaturedApps(appName, pageNum, pageSize);
        long total = appService.countFeaturedApps(appName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", appVOList);
        result.put("total", total);
        
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据id删除任意应用
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        
        boolean result = appService.adminDeleteApp(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据id更新任意应用（支持更新应用名称、应用封面、优先级）
     *
     * @param updateRequest 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AppAdminUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR);
        
        boolean result = appService.adminUpdateApp(updateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不限）
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> adminListApps(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        List<AppVO> appVOList = appService.adminListApps(appQueryRequest);
        long total = appService.adminCountApps(appQueryRequest);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", appVOList);
        result.put("total", total);
        
        return ResultUtils.success(result);
    }
    /**
    * 应用聊天生成代码（流式 SSE）
    *
    * @param appId   应用 ID
    * @param message 用户消息
    * @param request 请求对象
    * @return 生成结果流
    */

    @RateLimit(limitType = RateLimitType.USER,rate = 5,rateInterval = 60)
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        try {
            // 参数校验
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
            ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(request);
            // 调用服务生成代码（流式）
            Flux<String> stringFlux = appService.chatToGenCode(appId, message, loginUser);
            return stringFlux
                    .map(chunk -> {
                        Map<String, String> data = new HashMap<>();
                        data.put("data", chunk);
                        String jsonStr = JSONUtil.toJsonStr(data);
                        return ServerSentEvent.<String>builder()
                                .data(jsonStr)
                                .build();
                    })
                    .onErrorResume(e -> {
                        log.error("流式生成代码异常", e);
                        Map<String, String> errorData = new HashMap<>();
                        errorData.put("error", e.getMessage() != null ? e.getMessage() : "生成失败");
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data(JSONUtil.toJsonStr(errorData))
                                .build());
                    });
        } catch (Exception e) {
            log.error("启动流式生成失败", e);
            Map<String, String> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "生成失败");
            return Flux.just(ServerSentEvent.<String>builder()
                    .data(JSONUtil.toJsonStr(errorData))
                    .build());
        }
    }
    /**
     * 应用事件流生成代码（流式 SSE）
     * @param appId 应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @RateLimit(limitType = RateLimitType.USER,rate = 5,rateInterval = 60)
    @GetMapping(value = "/chat/event/code", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter genCode(@RequestParam Long appId,
                              @RequestParam String message,
                              HttpServletRequest request) {
        try {
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
            ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
            User loginUser = userService.getLoginUser(request);
            log.info("事件流接口: 开始执行，appId={}", appId);
            SseEmitter emitter = codeGenWorkflow.executeSse(message, appId, loginUser);
            log.info("事件流接口: 已返回 SseEmitter");
            return emitter;
        } catch (Exception e) {
            log.error("启动事件流生成失败", e);
            SseEmitter emitter = new SseEmitter();
            try {
                WorkflowEvent errorEvent = new WorkflowEvent();
                errorEvent.setEventType(WorkflowEvent.TYPE_ERROR);
                errorEvent.setContent(e.getMessage() != null ? e.getMessage() : "生成失败");
                Map<String, String> errorData = new HashMap<>();
                errorData.put("data", JSONUtil.toJsonStr(errorEvent));
                emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(errorData)));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            return emitter;
        }
    }
    
    /**
     * 测试 SSE 端点：直接发 3 个简单事件，验证基础通道
     */
    @GetMapping(value = "/chat/event/test", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter testSse() {
        log.info("测试 SSE 端点被调用");
        SseEmitter emitter = new SseEmitter(0L);
        Thread.startVirtualThread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    Thread.sleep(500);
                    WorkflowEvent event = new WorkflowEvent();
                    event.setEventType("step");
                    event.setStepName("测试步骤" + i);
                    Map<String, String> data = new HashMap<>();
                    data.put("data", JSONUtil.toJsonStr(event));
                    emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(data)));
                    log.info("测试 SSE 发送事件 {}", i);
                }
                emitter.complete();
                log.info("测试 SSE 完成");
            } catch (Exception e) {
                log.error("测试 SSE 异常", e);
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 管理员根据id查看应用详情
     *
     * @param id 应用id
     * @return 应用详情
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> adminGetAppDetail(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.adminGetAppDetail(id);
        return ResultUtils.success(app);
    }

    /**
     * 保存应用。
     *
     * @param app 应用
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody App app) {
        return appService.save(app);
    }

    /**
     * 根据主键删除应用。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return appService.removeById(id);
    }

    /**
     * 根据主键更新应用。
     *
     * @param app 应用
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody App app) {
        return appService.updateById(app);
    }

    /**
     * 查询所有应用。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<App> list() {
        return appService.list();
    }

    @Resource
    private ProjectDownloadService projectDownloadService;
    /**
     * 根据主键获取应用。
     *
     * @param id 应用主键
     * @return 应用详情
     */
    @GetMapping("getInfo/{id}")
    public App getInfo(@PathVariable Long id) {
        return appService.getById(id);
    }
    /**
     * 下载项目为zip文件
     * @param appId 项目id
     * @param
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,HttpServletRequest request, HttpServletResponse response) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "没有权限");

        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);

        ThrowUtils.throwIf(!sourceDir.exists()|| !sourceDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "项目不存在");
        String projectName = String.valueOf(appId);
        projectDownloadService.downloadProjcetAsZip(sourceDirPath, projectName, response);
    }

    /**
     * 查看最好app列表
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.fu.fuaicode.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
          // 校验查询参数
        if(appQueryRequest == null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
                
        }
        // 限制一次查询50条数据
        if(appQueryRequest.getPageSize() > 50){
            appQueryRequest.setPageSize(50);
        }
        // 调用服务查询最好app列表
        Page<AppVO> page = appService.listGoodAppVOByPage(appQueryRequest);
        return ResultUtils.success(page);
    }

}