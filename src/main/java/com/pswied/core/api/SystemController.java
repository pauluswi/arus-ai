package com.pswied.core.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SystemController {

    @GetMapping
    public ApiInfo info() {
        return new ApiInfo("ARUS AI", "Intelligent Payment Operations Platform", "1.0");
    }

    public record ApiInfo(String name, String description, String apiVersion) {
    }
}
