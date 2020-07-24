package pers.adlered.bce.ddns;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <h3>bce_ddns</h3>
 * <p>百度智能云 DDNS 主方法.</p>
 *
 * @author : https://github.com/adlered
 * @date : 2020-07-24
 **/
public class BaiduCloud {

    private static String authStringPrefix = "bce-auth-v1/{accessKeyId}/{timestamp}/{expirationPeriodInSeconds}";
    private static String canonicalRequest = "{httpMethod}" + "\n" + "{canonicalURI}" + "\n" + "{canonicalQueryString}" + "\n" + "{canonicalHeaders}";

    private static final String AK = "35e5d39ce0cc4370ae2e5ba6688fecb4";
    private static final String SK = "0fc6d606526c424ba1f6da03662b7e4b";

    public static void main(String[] args) throws Exception {
        // ### 1. AuthStringPrefix 前缀字符串 ###
        // AccessKeyId
        setAuthStringPrefix("accessKeyId", AK);
        // TimeStamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
        setAuthStringPrefix("timestamp", timestamp);
        // ExpirationPeriodInSeconds
        setAuthStringPrefix("expirationPeriodInSeconds", 3600);

        // ### 2. Canonical Request 规范请求 ###
        // HTTP Method
        setCanonicalRequest("httpMethod", "POST");
        // CanonicalURI
        String queryUri = "/v1/domain/resolve/list";
        String addUri = "/v1/domain/resolve/add";
        String updateUri = "/v1/domain/resolve/edit";
        String canonicalUri = URLEncoder.encode(queryUri, "UTF-8");
        canonicalUri = canonicalUri.replaceAll("%2F", "/");
        setCanonicalRequest("canonicalURI", canonicalUri);
        // CanonicalQueryString
        setCanonicalRequest("canonicalQueryString", "" +
                URLEncoder.encode("domain") + "=" + URLEncoder.encode("stackoverflow.wiki", "UTF-8"));
        // CanonicalHeaders
        String canonicalHeaders = "host:bcd.baidubce.com\nx-bce-date:" + timestamp;
        setCanonicalRequest("canonicalHeaders", canonicalHeaders);
        // SignedHeaders 签名头域
        String signedHeaders = "";
        // SigningKey 派生密钥
        String signingKey = hMacSha256(authStringPrefix, SK);
        System.out.println("SigningKey = " + signingKey);
        // Signature 签名摘要
        String signature = hMacSha256(canonicalRequest, signingKey);
        System.out.println("Signature = " + signature);

        // ### Authorization 认证字符串 ###
        String authorization = authStringPrefix + "/" + signedHeaders + "/" + signature;
        System.out.println("Authorization = " + authorization);
    }

    private static void setAuthStringPrefix(String key, Object value) {
        authStringPrefix = authStringPrefix.replaceAll("\\{" + key + "\\}", value.toString());
        System.out.println("[AuthStringPrefix] " + authStringPrefix);
    }

    private static void setCanonicalRequest(String key, Object value) {
        canonicalRequest = canonicalRequest.replaceAll("\\{" + key + "\\}", value.toString());
        System.out.println("*** [CanonicalRequest START] ***\n" + canonicalRequest + "\n*** [CanonicalRequest END] ***");
    }

    public static String hMacSha256(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] array = sha256Hmac.doFinal(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte item : array) {
            sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
