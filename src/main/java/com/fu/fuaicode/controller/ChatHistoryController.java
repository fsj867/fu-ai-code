package com.fu.fuaicode.controller;

import com.fu.fuaicode.common.BaseResponse;
import com.fu.fuaicode.common.ResultUtils;
import com.fu.fuaicode.model.entity.ChatHistory;
import com.fu.fuaicode.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话历史 控制层。
 *
 * @author fsj
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 保存用户消息
     *
     * @param appId   应用id
     * @param userId  用户id
     * @param message 消息内容
     * @return 保存的对话历史记录
     */
    @PostMapping("/user/save")
    public BaseResponse<ChatHistory> saveUserMessage(@RequestParam Long appId,
                                                     @RequestParam Long userId,
                                                     @RequestParam String message) {
        ChatHistory chatHistory = chatHistoryService.saveUserMessage(appId, userId, message);
        return ResultUtils.success(chatHistory);
    }

    /**
     * 保存AI消息
     *
     * @param appId   应用id
     * @param userId  用户id
     * @param message 消息内容
     * @return 保存的对话历史记录
     */
    @PostMapping("/ai/save")
    public BaseResponse<ChatHistory> saveAiMessage(@RequestParam Long appId,
                                                   @RequestParam Long userId,
                                                   @RequestParam String message) {
        ChatHistory chatHistory = chatHistoryService.saveAiMessage(appId, userId, message);
        return ResultUtils.success(chatHistory);
    }

    /**
     * 保存错误消息
     *
     * @param appId         应用id
     * @param userId        用户id
     * @param errorMessage  错误消息内容
     * @return 保存的对话历史记录
     */
    @PostMapping("/error/save")
    public BaseResponse<ChatHistory> saveErrorMessage(@RequestParam Long appId,
                                                      @RequestParam Long userId,
                                                      @RequestParam String errorMessage) {
        ChatHistory chatHistory = chatHistoryService.saveErrorMessage(appId, userId, errorMessage);
        return ResultUtils.success(chatHistory);
    }

    /**
     * 根据应用ID分页查询对话历史（向前加载）
     *
     * @param appId    应用id
     * @param lastId   最后一条消息的id（用于向前加载，不传则加载最新的）
     * @param pageSize 每页数量，默认10条
     * @return 对话历史列表
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<List<ChatHistory>> listByAppId(@PathVariable Long appId,
                                                       @RequestParam(required = false) Long lastId,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        List<ChatHistory> chatHistoryList = chatHistoryService.listByAppId(appId, lastId, pageSize);
        return ResultUtils.success(chatHistoryList);
    }

    /**
     * 根据应用ID删除所有对话历史
     *
     * @param appId 应用id
     * @return 删除的数量
     */
    @DeleteMapping("/app/{appId}")
    public BaseResponse<Integer> deleteByAppId(@PathVariable Long appId) {
        int count = chatHistoryService.deleteByAppId(appId);
        return ResultUtils.success(count);
    }

    /**
     * 获取应用的对话历史总数
     *
     * @param appId 应用id
     * @return 对话历史总数
     */
    @GetMapping("/app/{appId}/count")
    public BaseResponse<Long> countByAppId(@PathVariable Long appId) {
        long count = chatHistoryService.countByAppId(appId);
        return ResultUtils.success(count);
    }

    /**
     * 管理员分页查询所有对话历史（按时间降序）
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页数量，默认10
     * @return 对话历史列表
     */
    @GetMapping("/admin/list")
    public BaseResponse<List<ChatHistory>> listAllForAdmin(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        List<ChatHistory> chatHistoryList = chatHistoryService.listAllForAdmin(pageNum, pageSize);
        return ResultUtils.success(chatHistoryList);
    }

    /**
     * 管理员获取所有对话历史总数
     *
     * @return 对话历史总数
     */
    @GetMapping("/admin/count")
    public BaseResponse<Long> countAllForAdmin() {
        long count = chatHistoryService.countAllForAdmin();
        return ResultUtils.success(count);
    }

    /**
     * 根据主键获取对话历史详情
     *
     * @param id 对话历史主键
     * @return 对话历史详情
     */
    @GetMapping("/getInfo/{id}")
    public BaseResponse<ChatHistory> getInfo(@PathVariable Long id) {
        ChatHistory chatHistory = chatHistoryService.getById(id);
        return ResultUtils.success(chatHistory);
    }
}
