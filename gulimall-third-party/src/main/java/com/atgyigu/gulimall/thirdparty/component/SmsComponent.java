package com.atgyigu.gulimall.thirdparty.component;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.models.*;
import com.aliyun.sdk.service.dysmsapi20170525.*;
import com.google.gson.Gson;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 验证码发送
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms")
public class SmsComponent {

  private String accessKeyId;  // accessKeyId
  private String accessKeySecret; // accessKeySecret
  private String signName;  // 短信签名    // HappyWztXsy1
  private String templateCode; // 短信模板  // SMS_270170156

  /**
   * @param phone 手机号
   * @param code  验证码
   */
  public void sendSmsCode(String phone, String code) throws Exception {
    // Configure Credentials authentication information, including ak, secret, token
    StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
        .accessKeyId(accessKeyId)
        .accessKeySecret(accessKeySecret)
        //.securityToken("<your-token>") // use STS token
        .build());

    // Configure the Client
    AsyncClient client = AsyncClient.builder()
        .region("cn-hangzhou") // Region ID
        //.httpClient(httpClient) // Use the configured HttpClient, otherwise use the default HttpClient (Apache HttpClient)
        .credentialsProvider(provider)
        //.serviceConfiguration(Configuration.create()) // Service-level configuration
        // Client-level configuration rewrite, can set Endpoint, Http request parameters, etc.
        .overrideConfiguration(
            ClientOverrideConfiguration.create()
                .setEndpointOverride("dysmsapi.aliyuncs.com")
            //.setConnectTimeout(Duration.ofSeconds(30))
        )
        .build();

    // Parameter settings for API request
    SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
        .signName(signName)
        .templateCode(templateCode)
        .templateParam("{\"code\":\"" + code + "\"}")  // code验证码
        .phoneNumbers(phone)  // 手机号
        // Request-level configuration rewrite, can set Http request parameters, etc.
        // .requestConfiguration(RequestConfiguration.create().setHttpHeaders(new HttpHeaders()))
        .build();

    // Asynchronously get the return value of the API request
    CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);
    // Synchronously get the return value of the API request
    SendSmsResponse resp = response.get();
    System.out.println(new Gson().toJson(resp));
    // Asynchronous processing of return values
        /*response.thenAccept(resp -> {
            System.out.println(new Gson().toJson(resp));
        }).exceptionally(throwable -> { // Handling exceptions
            System.out.println(throwable.getMessage());
            return null;
        });*/

    // Finally, close the client
    client.close();
  }


//  private String host; // https://dfsns.market.alicloudapi.com
//  private String path; // /data/send_sms
//  private String appcode; // 6860238ecbf04dbfb51bdaa8ca76544b
//  private String templateId; // TPL_0001
//  private String content;  // ,expire_at:5
//
//  /**
//   * @param phone  手机号
//   * @param code  验证码
//   */
//  public void sendSmsCode(String phone, String code) {
//    String method = "POST";
//    Map<String, String> headers = new HashMap<String, String>();
//    //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
//    headers.put("Authorization", "APPCODE " + appcode);
//    //根据API的要求，定义相对应的Content-Type
//    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
//    Map<String, String> querys = new HashMap<String, String>();
//    Map<String, String> bodys = new HashMap<String, String>();
//    if (content != null && !StringUtils.isEmpty(content)) {
//      bodys.put("content", "code:" + code + "," + content);
//    } else {
//      bodys.put("content", "code:" + code);
//    }
//    bodys.put("phone_number", phone);
//    bodys.put("template_id", templateId);
//    try {
//      HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
//      System.out.println(response.toString());
//      //获取response的body
//      //System.out.println(EntityUtils.toString(response.getEntity()));
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
}
