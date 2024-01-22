package com.sky.utils;


import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Data
@AllArgsConstructor
@Slf4j
public class QiNiuOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;

    /**
     * 文件上传
     *
     * @param bytes    文件内容
     * @param objectName 对象名称
     * @return 文件访问路径
     */
    public String upload(byte[] bytes, String objectName) {
        // 创建上传manager对象
        UploadManager uploadManager = new UploadManager(new Configuration(Region.region2()));
        // 创建一个Auth对象，用于生成签名
        Auth auth = Auth.create(accessKeyId, accessKeySecret);

        try {
            // 生成上传文件的凭证
            String upToken = auth.uploadToken(bucket);

            // 调用put方法上传
            Response response = uploadManager.put(bytes, objectName, upToken);

            // 解析上传成功的结果
            if (response.isOK()) {
//                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
//                // 获取文件访问URL
//                String fileUrl = putRet.hash;

//                log.info("文件上传到： {}", fileUrl);
                StringBuilder stringBuilder = new StringBuilder("http://");
                stringBuilder
                        .append(endpoint)
                        .append("/")
                        .append(objectName);
                log.info("文件上传到: {}", stringBuilder.toString());
                return stringBuilder.toString();
            } else {
                // 如果响应不成功，抛出异常
                throw new QiniuException(response);
            }
        } catch (QiniuException ex) {
            log.error("Caught a QiniuException: {}", ex.getMessage());
        }
        return null;
    }
}

