package com.fu.fuaicode.manager;

import com.fu.fuaicode.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS对象管理器
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;

    /**
     * 上传文件
     * @param key 唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult uploadFile(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file); // 创建上传请求
        return cosClient.putObject(putObjectRequest); // 上传文件
    }
    /**
     * 上传文件到COS 并返回url
     * @param key 唯一键
     * @param file 文件
     * @return url
     */
    public String uploadFileAndGetUrl(String key, File file) {
        PutObjectResult putObjectResult = uploadFile(key, file);
        if (putObjectResult != null){
            String URL = String.format("%s/%s", cosClientConfig.getHost(), key);
            log.info("上传文件成功，URL: {}", URL);
            return URL;
        }else {
            log.error("上传文件失败");
            return null;
        }
    }
}
