package com.fu.fuaicode.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fu.fuaicode.common.BaseResponse;
import com.fu.fuaicode.common.ResultUtils;

@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping("/")
    public BaseResponse<String> health() {
        return ResultUtils.success("OK");
    }
}
