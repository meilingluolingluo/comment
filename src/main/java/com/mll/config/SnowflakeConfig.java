package com.mll.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SnowflakeConfig {

    @Value("${worker.id}")
    private long workerId;

    @Value("${datacenter.id}")
    private long datacenterId;

}