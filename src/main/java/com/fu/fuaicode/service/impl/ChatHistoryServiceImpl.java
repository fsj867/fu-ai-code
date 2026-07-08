package com.fu.fuaicode.service.impl;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;

import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.mapper.ChatHistoryMapper;
import com.fu.fuaicode.model.entity.ChatHistory;
import com.fu.fuaicode.model.enums.MessageTypeEnum;
import com.fu.fuaicode.service.ChatHistoryService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author fsj
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    @Override
    public ChatHistory saveUserMessage(Long appId, Long userId, String message) {
        validateParams(appId, userId, message);
        return saveMessage(appId, userId, message, MessageTypeEnum.USER);
    }

    @Override
    public ChatHistory saveAiMessage(Long appId, Long userId, String message) {
        validateParams(appId, userId, message);
        return saveMessage(appId, userId, message, MessageTypeEnum.AI);
    }

    @Override
    public ChatHistory saveErrorMessage(Long appId, Long userId, String errorMessage) {
        validateParams(appId, userId, errorMessage);
        return saveMessage(appId, userId, errorMessage, MessageTypeEnum.ERROR);
    }

    @Override
    public List<ChatHistory> listByAppId(Long appId, Long lastId, int pageSize) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }

        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            pageSize = 10;
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("appId", appId)
                .orderBy("createTime", false);

        if (lastId != null && lastId > 0) {
            ChatHistory lastHistory = this.mapper.selectOneById(lastId);
            if (lastHistory != null) {
                queryWrapper.lt("createTime", lastHistory.getCreateTime());
            }
        }

        queryWrapper.limit(pageSize);

        return this.mapper.selectListByQuery(queryWrapper);
    }

    @Override
    public int deleteByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("appId", appId);

        return this.mapper.deleteByQuery(queryWrapper);
    }

    @Override
    public List<ChatHistory> listAllForAdmin(int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            pageSize = 10;
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .orderBy("createTime", false)
                .offset((pageNum - 1) * pageSize)
                .limit(pageSize);

        return this.mapper.selectListByQuery(queryWrapper);
    }

    @Override
    public long countByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }

        QueryWrapper queryWrapper = new QueryWrapper()
                .eq("appId", appId);

        return this.mapper.selectCountByQuery(queryWrapper);
    }

    @Override
    public long countAllForAdmin() {
        QueryWrapper queryWrapper = new QueryWrapper();
        return this.mapper.selectCountByQuery(queryWrapper);
    }

    /**
     * 校验参数
     */
    private void validateParams(Long appId, Long userId, String message) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id无效");
        }
        if (StrUtil.isBlank(message)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
    }

    /**
     * 保存消息
     */
    private ChatHistory saveMessage(Long appId, Long userId, String message, MessageTypeEnum messageType) {
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAppId(appId);
        chatHistory.setUserId(userId);
        chatHistory.setMessage(message);
        chatHistory.setMessageType(messageType.getValue());
        chatHistory.setCreateTime(LocalDateTime.now());
        chatHistory.setUpdateTime(LocalDateTime.now());

        boolean save = this.save(chatHistory);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存对话历史失败");
        }

        return chatHistory;
    }
    /**
     * 加载对话历史到内存
     * @param appId 应用id
     * @param chatMemory 对话历史内存
     * @param maxCount 最大加载条数
     * @return 加载的条数
     */
    @Override
    public int loadChatHistortToMemory(long appId,MessageWindowChatMemory chatMemory,int maxCount) {
        try{
            //直接构成查询方法，起点为1，忽略用户最新消息
            QueryWrapper queryWrapper = new QueryWrapper()
                    .eq("appId", appId)
                    .orderBy(ChatHistory::getCreateTime, false) //按创建时间升序排序
                    .limit(1,maxCount); // 排除第一条消息
             List<ChatHistory> selectListByQuery = this.mapper.selectListByQuery(queryWrapper); 
             if (selectListByQuery.isEmpty()) {
                 return 0;
             }      
             //反转列表，确保最新消息在最前面
             Collections.reverse(selectListByQuery);
             int loadCount = 0;
             //清空内存
             chatMemory.clear();
             for (ChatHistory chatHistory : selectListByQuery) {
                 if (MessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                     loadCount++;
                 }
                 if(MessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())){
                    chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                     loadCount++;
                 }
             }
             log.info("成功为{}加载对话历史成功，共加载{}条消息",appId,loadCount);
             return loadCount;
        }catch (Exception e){
            log.debug("加载对话历史失败",e);
            return 0;
        }
    }
}
