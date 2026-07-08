package com.fu.fuaicode.ai.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fu.fuaicode.langgraph4j.state.ImageResource;
import com.fu.fuaicode.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ImageSearchTool {

    private static final String BASE_URL = "https://api.pexels.com/v1/search";

    @Value("${pexels.api.key:}")
    private String apiKey;

    @Tool("搜索内容相关图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(String query){
        List<ImageResource> imageResources = new ArrayList<>();
        int searchCount = 12;
        try(HttpResponse response = HttpRequest.get(BASE_URL)
                    .header("Authorization", apiKey)
                    .form("query", query)
                    .form("per_page", searchCount)
                    .form("page",1).execute()) {
            if (response.isOk()){
                JSONObject jsonObject = JSONUtil.parseObj(response.body()); // 解析json
                JSONArray photos = jsonObject.getJSONArray("photos");
                // 遍历图片
                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = JSONUtil.parseObj(photos.get(i));
                    JSONObject src = JSONUtil.parseObj(photo.get("src"));
                    imageResources.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(photo.getStr("alt",query))
                                    .url(src.getStr("medium"))
                                    .build()
                            );
                }
            }

        }catch (Exception e){
            log.error("searchContentImages error",e);
        }
        return imageResources;
    }
}
