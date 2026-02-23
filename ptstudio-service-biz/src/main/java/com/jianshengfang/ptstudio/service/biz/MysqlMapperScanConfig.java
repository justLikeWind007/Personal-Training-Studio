package com.jianshengfang.ptstudio.service.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mysql")
@MapperScan("com.jianshengfang.ptstudio.core.infrastructure")
public class MysqlMapperScanConfig {
}
