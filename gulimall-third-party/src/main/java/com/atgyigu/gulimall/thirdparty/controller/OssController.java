package com.atgyigu.gulimall.thirdparty.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import io.renren.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("oss")
public class OssController {
  @Autowired
  private OSS ossClient;

  @Value("${spring.cloud.alicloud.oss.endpoint}")
  private String endpoint;

  @Value("${spring.cloud.alicloud.oss.bucket}")
  private String bucket;

  @Value("${spring.cloud.alicloud.access-key}")
  private String accessKeyId;

  @RequestMapping("/policy")
  public R policy() {
/*
          一下消信息在nacos配置中心中配置了
    // Endpoint以杭州为例，其它Region请按实际情况填写。
    String endpoint = "oss-cn-beijing.aliyuncs.com";
    // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
    String accessKeyId = "LTAI4G66cCNM2t7LKE79RaY3";
    String accessKeySecret = "wd0KVDLCO1vVXq4q9aIPTXY7AP7rdW";*/

//    https://gulimall-happy-wzt.oss-cn-nanjing.aliyuncs.com/backg2.jpg
//    String bucket = "gulimall-happy-wzt"; // 请填写您的 bucketname 。
    String host = "https://" + bucket + "." + endpoint; // host的格式为 bucketname.endpoint    URL
    // callbackUrl为 上传回调服务器的URL，请将下面的IP和Port配置为您自己的真实信息。
//    String callbackUrl = "http://88.88.88.88:8888";

    //                                           年  月   日         天
    String format = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    // 表示在同一天上传的文件 放在同一个文件夹中(文件夹的名称是当天的日期)
    String dir = format + "/"; // 用户上传文件时指定的前缀      也就是目录

    // 创建OSSClient实例。
//    OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    Map<String, String> respMap = null;
    try {
      long expireTime = 30;
      long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
      Date expiration = new Date(expireEndTime);
      PolicyConditions policyConds = new PolicyConditions();
      policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
      policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

      String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
      byte[] binaryData = postPolicy.getBytes("utf-8");
      String encodedPolicy = BinaryUtil.toBase64String(binaryData);
      String postSignature = ossClient.calculatePostSignature(postPolicy);

      respMap = new LinkedHashMap<String, String>();
      respMap.put("accessid", accessKeyId);
      respMap.put("policy", encodedPolicy);
      respMap.put("signature", postSignature);
      respMap.put("dir", dir);
      respMap.put("host", host);
      respMap.put("expire", String.valueOf(expireEndTime / 1000));

    } catch (Exception e) {
      // Assert.fail(e.getMessage());
      System.out.println(e.getMessage());
    }
    return R.ok().put("data", respMap);
  }

}
