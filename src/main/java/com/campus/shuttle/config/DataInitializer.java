package com.campus.shuttle.config;

import com.campus.shuttle.service.ShuttleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ShuttleService shuttleService;

    @Override
    public void run(String... args) throws Exception {
        shuttleService.initializeSchedules();
        System.out.println("資料初始化完成");
    }
}