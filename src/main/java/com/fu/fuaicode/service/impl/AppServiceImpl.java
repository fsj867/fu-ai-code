package com.fu.fuaicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.fu.fuaicode.ai.AiCodeGenTypeRoutingService;
import com.fu.fuaicode.constant.AppConstant;
import com.fu.fuaicode.model.dto.app.AppAddRequest;
import com.fu.fuaicode.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import com.fu.fuaicode.core.AiCodeGeneratorFacade;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.exception.ThrowUtils;
import com.fu.fuaicode.model.entity.User;
import com.fu.fuaicode.mapper.AppMapper;
import com.fu.fuaicode.model.dto.app.AppAdminUpdateRequest;
import com.fu.fuaicode.model.dto.app.AppQueryRequest;
import com.fu.fuaicode.model.dto.app.AppUpdateRequest;
import com.fu.fuaicode.model.entity.App;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import com.fu.fuaicode.model.vo.AppVO;
import com.fu.fuaicode.service.AppService;
import com.fu.fuaicode.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author fsj
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private static final int MAX_PAGE_SIZE = 20;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    @Override
    public Long createApp(AppAddRequest appAddRequest, Long userId) {
        ThrowUtils.throwIf(appAddRequest.getInitPrompt() == null, ErrorCode.PARAMS_ERROR,"初始化提示不能为空");
        String initPrompt = appAddRequest.getInitPrompt();
        CodeGenTypeEnum codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        App app = new App();
        BeanUtil.copyProperties(appAddRequest,app);
        app.setUserId(userId);
        app.setCodeGenType(codeGenTypeEnum.getValue());
        boolean save = this.save(app);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建应用失败，数据库错误");
        }
        log.info("创建应用成功,ID = {}, 类型为{}", app.getId(), codeGenTypeEnum.getValue());
        return app.getId();
    }

    @Override
    public boolean updateApp(AppUpdateRequest appUpdateRequest, Long userId) {
        if (appUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long appId = appUpdateRequest.getId();
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id无效");
        }

        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        if (!userId.equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改此应用");
        }

        String appName = appUpdateRequest.getAppName();
        if (StrUtil.isNotBlank(appName)) {
            app.setAppName(appName);
        }
        app.setUpdateTime(LocalDateTime.now());

        return this.updateById(app);
    }

    @Override
    public boolean deleteApp(Long appId, Long userId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id无效");
        }

        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        if (!userId.equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除此应用");
        }

        chatHistoryService.deleteByAppId(appId);

        return this.removeById(appId);
    }

    @Override
    public App getAppDetail(Long appId, Long userId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id无效");
        }

        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        if (!userId.equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看此应用");
        }

        return app;
    }

    @Override
    public List<AppVO> listUserApps(Long userId, String appName, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id无效");
        }

        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("userId", userId)
                .like("appName", appName)
                .orderBy("createTime", false)
                .offset((pageNum - 1) * pageSize)
                .limit(pageSize);

        List<App> appList = this.mapper.selectListByQuery(queryWrapper);
        return convertToVOList(appList);
    }

    @Override
    public List<AppVO> listFeaturedApps(String appName, int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .gt("priority", 0)
                .like("appName", appName)
                .orderBy("priority", true)
                .orderBy("createTime", false)
                .offset((pageNum - 1) * pageSize)
                .limit(pageSize);

        List<App> appList = this.mapper.selectListByQuery(queryWrapper);
        return convertToVOList(appList);
    }

    @Override
    public boolean adminDeleteApp(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }

        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        chatHistoryService.deleteByAppId(appId);

        return this.removeById(appId);
    }

    @Override
    public boolean adminUpdateApp(AppAdminUpdateRequest updateRequest) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long appId = updateRequest.getId();
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }

        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        String appName = updateRequest.getAppName();
        if (StrUtil.isNotBlank(appName)) {
            app.setAppName(appName);
        }

        String cover = updateRequest.getCover();
        if (cover != null) {
            app.setCover(cover);
        }

        Integer priority = updateRequest.getPriority();
        if (priority != null) {
            app.setPriority(priority);
        }

        app.setUpdateTime(LocalDateTime.now());

        return this.updateById(app);
    }

    @Override
    public List<AppVO> adminListApps(AppQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();

        if (pageNum < 1) {
            pageNum = 1;
        }

        QueryWrapper queryWrapper = getQueryWrapper(queryRequest)
                .offset((pageNum - 1) * pageSize)
                .limit(pageSize);

        List<App> appList = this.mapper.selectListByQuery(queryWrapper);
        return convertToVOList(appList);
    }

    @Override
    public App adminGetAppDetail(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }

        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        return app;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String codeGenType = appQueryRequest.getCodeGenType();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = new QueryWrapper();

        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        if (StrUtil.isNotBlank(appName)) {
            queryWrapper.like("appName", appName);
        }
        if (StrUtil.isNotBlank(cover)) {
            queryWrapper.like("cover", cover);
        }
        if (StrUtil.isNotBlank(codeGenType)) {
            queryWrapper.eq("codeGenType", codeGenType);
        }
        if (priority != null && priority > 0) {
            queryWrapper.eq("priority", priority);
        }
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }

        if (StrUtil.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(sortField, isAsc);
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }
    @Override
    public String deployApp(Long appId, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0,ErrorCode.PARAMS_ERROR,"应用id无效");
        //获取应用
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null,ErrorCode.NOT_FOUND_ERROR,"应用不存在");
        //校验权限
        if(!app.getUserId().equals(loginUser.getId())){

            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,"没有权限调用该应用");
        }
        //检查是否有deploykey
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        //获得源目录
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if(!sourceDir.exists()||!sourceDir.isDirectory()){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"源目录不存在,请先生成代码");
        }
        // vue 项目特殊处理
        if(codeGenType.equals("vue_project")){
            File distDir = new File(sourceDir, "dist");
            if (!distDir.exists() && !distDir.isDirectory()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"dist目录不存在");
            }
            sourceDir = distDir; // vue 项目特殊处理
            log.info("vue项目构建成功，将部署dist目录：{}", distDir.getAbsolutePath());
        }
        //获得部署目录
        String deployPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try{
            FileUtil.copyContent(sourceDir,new File(deployPath), true);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"部署失败" + e.getMessage());
        }
        String deployUrl = String.format("%s/%s/",AppConstant.CODE_DEPLOY_HOST,deployKey);
        //生成封面
        generateAppScreenshotAsync(appId,deployUrl);
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setDeployKey(deployKey);
        boolean updateById = this.updateById(updateApp);
        if (!updateById){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新应用失败");
        }
        //返回可访问的URL
        return String.format("%s/%s/",AppConstant.CODE_DEPLOY_HOST,deployKey);
    }
    @Override
    public long countUserApps(Long userId, String appName) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id无效");
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("userId", userId)
                .like("appName", appName);

        return this.mapper.selectCountByQuery(queryWrapper);
    }

    @Override
    public long countFeaturedApps(String appName) {
        QueryWrapper queryWrapper = new QueryWrapper()
                .gt("priority", 0)
                .like("appName", appName);

        return this.mapper.selectCountByQuery(queryWrapper);
    }

    @Override
    public long adminCountApps(AppQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper queryWrapper = getQueryWrapper(queryRequest);
        return this.mapper.selectCountByQuery(queryWrapper);
    }

    /**
     * 将应用列表转换为视图对象列表
     */
    private List<AppVO> convertToVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        return appList.stream()
                .map(AppVO::fromEntity)
                .collect(Collectors.toList());
    }
    /**
     * 调用门面生成代码（流式返回）
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0,ErrorCode.PARAMS_ERROR,"应用id无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message),ErrorCode.PARAMS_ERROR,"消息内容不能为空");
        //获取应用
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null,ErrorCode.NOT_FOUND_ERROR,"应用不存在");
        //校验权限
        if(!app.getUserId().equals(loginUser.getId())){
            
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,"没有权限调用该应用");
        }
        //获得应用代码生成类型
        String codeGenType = app.getCodeGenType();
        ThrowUtils.throwIf(StrUtil.isBlank(codeGenType),ErrorCode.PARAMS_ERROR,"应用代码生成类型不能为空");
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null,ErrorCode.PARAMS_ERROR,"应用代码生成类型无效");
        //调用门面生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
    }


    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }
    @Override
    public Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest) {
        if(appQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        }
        // 限制一次查询50条数据
        if(appQueryRequest.getPageSize() > 50){
            appQueryRequest.setPageSize(50);
        }
        // 使用 MyBatis-Flex 原生分页查询
        Page<App> appPage = this.mapper.paginate(
                appQueryRequest.getPageNum(),
                appQueryRequest.getPageSize(),
                getQueryWrapper(appQueryRequest)
        );
        
        // 转换为 VO
        Page<AppVO> voPage = new Page<>();
        voPage.setPageNumber(appPage.getPageNumber());
        voPage.setPageSize(appPage.getPageSize());
        voPage.setTotalRow(appPage.getTotalRow());
        voPage.setRecords(convertToVOList(appPage.getRecords()));
        return voPage;

    }
}