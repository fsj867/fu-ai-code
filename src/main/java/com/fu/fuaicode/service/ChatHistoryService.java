package com.fu.fuaicode.service;

import com.fu.fuaicode.model.entity.ChatHistory;
import com.mybatisflex.core.service.IService;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 * @author fsj
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存用户消息
     *
     * @param appId    应用id
     * @param userId   用户id
     * @param message  消息内容
     * @return 保存的对话历史记录
     */
    ChatHistory saveUserMessage(Long appId, Long userId, String message);

    /**
     * 保存AI消息
     *
     * @param appId    应用id
     * @param userId   用户id
     * @param message  消息内容
     * @return 保存的对话历史记录
     */
    ChatHistory saveAiMessage(Long appId, Long userId, String message);

    /**
     * 保存错误消息
     *
     * @param appId    应用id
     * @param userId   用户id
     * @param errorMessage 错误消息内容
     * @return 保存的对话历史记录
     */
    ChatHistory saveErrorMessage(Long appId, Long userId, String errorMessage);

    /**
     * 根据应用ID分页查询对话历史（向前加载，最新10条）
     *
     * @param appId    应用id
     * @param lastId   最后一条消息的id（用于向前加载，不传则加载最新的）
     * @param pageSize 每页数量
     * @return 对话历史列表，按时间降序排列
     */
    List<ChatHistory> listByAppId(Long appId, Long lastId, int pageSize);

    /**
     * 根据应用ID删除所有对话历史
     *
     * @param appId 应用id
     * @return 删除的数量
     */
    int deleteByAppId(Long appId);

    /**
     * 管理员分页查询所有对话历史（按时间降序）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 对话历史列表
     */
    List<ChatHistory> listAllForAdmin(int pageNum, int pageSize);

    /**
     * 获取应用的对话历史总数
     *
     * @param appId 应用id
     * @return 对话历史总数
     */
    long countByAppId(Long appId);

    /**
     * 管理员获取所有对话历史总数
     *
     * @return 对话历史总数
     */
    long countAllForAdmin();

    int loadChatHistortToMemory(long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
