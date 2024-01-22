package com.sky.config;

import com.sky.properties.QiNiuOssProperties;
import com.sky.utils.QiNiuOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于创建QiNiuUtil对象
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QiNiuOssUtil qiNiuOssUtil(QiNiuOssProperties qiNiuOssProperties){
        log.info("创建七牛云上传图片工具类对象: {}",qiNiuOssProperties);
        return new QiNiuOssUtil(qiNiuOssProperties.getEndpoint(),
                qiNiuOssProperties.getAccessKeyId(),
                qiNiuOssProperties.getAccessKeySecret(),
                qiNiuOssProperties.getBucket());
    }
}
