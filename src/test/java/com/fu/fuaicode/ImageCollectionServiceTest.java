package com.fu.fuaicode;

import com.fu.fuaicode.ai.ImageCollectionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
class ImageCollectionServiceTest {

    @Resource
    private ImageCollectionService imageCollectionService;


    @Test
    void testEcommerceWebsiteImageCollection() {
        String result = imageCollectionService.generateImageCollection("创建一个电商购物网站，需要展示商品和品牌形象");
        Assertions.assertNotNull(result);
        System.out.println("电商网站收集到的图片: " + result);
    }
}
