package com.fu.fuaicode.service;


import com.fu.fuaicode.model.dto.app.AppAddRequest;
import com.fu.fuaicode.model.entity.User;
import com.fu.fuaicode.model.dto.app.AppAdminUpdateRequest;
import com.fu.fuaicode.model.dto.app.AppQueryRequest;
import com.fu.fuaicode.model.dto.app.AppUpdateRequest;
import com.fu.fuaicode.model.entity.App;
import com.fu.fuaicode.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author fsj
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     * @param request 请求对象
     * @param userId  用户id
     * @return 应用id
     */
    Long createApp(AppAddRequest request, Long userId);

    /**
     * 用户更新自己的应用（仅支持修改应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param userId           用户id
     * @return 是否更新成功
     */
    boolean updateApp(AppUpdateRequest appUpdateRequest, Long userId);

    /**
     * 用户删除自己的应用
     *
     * @param appId  应用id
     * @param userId 用户id
     * @return 是否删除成功
     */
    boolean deleteApp(Long appId, Long userId);

    /**
     * 用户查看应用详情
     *
     * @param appId  应用id
     * @param userId 用户id
     * @return 应用详情
     */
    App getAppDetail(Long appId, Long userId);

    /**
     * 用户分页查询自己的应用列表
     *
     * @param userId        用户id
     * @param appName       应用名称（可选）
     * @param pageNum       页码
     * @param pageSize      每页数量
     * @return 应用列表
     */
    List<AppVO> listUserApps(Long userId, String appName, int pageNum, int pageSize);

    /**
     * 用户分页查询精选应用列表
     *
     * @param appName  应用名称（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 应用列表
     */
    List<AppVO> listFeaturedApps(String appName, int pageNum, int pageSize);

    /**
     * 管理员删除任意应用
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean adminDeleteApp(Long appId);

    /**
     * 管理员更新任意应用（支持更新应用名称、应用封面、优先级）
     *
     * @param updateRequest 更新请求
     * @return 是否更新成功
     */
    boolean adminUpdateApp(AppAdminUpdateRequest updateRequest);

    /**
     * 管理员分页查询应用列表（支持根据除时间外的任何字段查询）
     *
     * @param queryRequest 查询请求
     * @return 应用列表
     */
    List<AppVO> adminListApps(AppQueryRequest queryRequest);

    /**
     * 管理员查看应用详情
     *
     * @param appId 应用id
     * @return 应用详情
     */
    App adminGetAppDetail(Long appId);

    /**
     * 获取查询条件封装
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    String deployApp(Long appId, User loginUser);

    /**
     * 获取用户应用总数
     *
     * @param userId  用户id
     * @param appName 应用名称（可选）
     * @return 总数
     */
    long countUserApps(Long userId, String appName);

    /**
     * 获取精选应用总数
     *
     * @param appName 应用名称（可选）
     * @return 总数
     */
    long countFeaturedApps(String appName);

    /**
     * 获取管理员查询应用总数
     *
     * @param queryRequest 查询请求
     * @return 总数
     */
    long adminCountApps(AppQueryRequest queryRequest);
    /**
     * 调用门面生成代码（流式返回）
     * @param appId 应用id
     * @param message 消息内容
     * @param loginUser 登录用户
     * @return 生成的代码
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);
    /**
     * 分页查询最好应用列表
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest);
}