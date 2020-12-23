package com.atguigu.gmall.pms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Data
@ConfigurationProperties(prefix = "oss")
@Component
public class OssConfig {

    private String accessId ; // 请填写您的AccessKeyId。
    private String accessKey ; // 请填写您的AccessKeySecret。
    private String endpoint; // 请填写您的 endpoint。
    private String bucket ; // 请填写您的 bucketname 。
    private String host;  // host的格式为 bucketname.endpoint


    @PostConstruct
    public void init(){
        host = "https://" + bucket + "." + endpoint; // host的格式为 bucketname.endpoint
    }
}
