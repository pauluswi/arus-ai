package com.pswied.core.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalDemoDataInitializer implements ApplicationRunner {

    private final DemoDataService demoDataService;
    private final boolean enabled;
    private final boolean reset;

    public LocalDemoDataInitializer(
            DemoDataService demoDataService,
            @Value("${arus.demo-data.enabled:true}") boolean enabled,
            @Value("${arus.demo-data.reset:false}") boolean reset) {
        this.demoDataService = demoDataService;
        this.enabled = enabled;
        this.reset = reset;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) {
            demoDataService.initialize(reset);
        }
    }
}
