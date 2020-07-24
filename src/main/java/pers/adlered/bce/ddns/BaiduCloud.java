package pers.adlered.bce.ddns;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
        Calendar calendar = Calendar.getInstance();
        int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        int dstOffset = calendar.get(Calendar.DST_OFFSET);
        calendar.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date(calendar.getTimeInMillis()));
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
        //setCanonicalRequest("canonicalQueryString", "" +
         //       URLEncoder.encode("domain") + "=" + URLEncoder.encode("stackoverflow.wiki", "UTF-8"));
        setCanonicalRequest("canonicalQueryString", "");
        // CanonicalHeaders
        String canonicalHeaders = "host:bcd.baidubce.com\nx-bce-date:" + timestamp.replaceAll(":", "%3A");
        setCanonicalRequest("canonicalHeaders", canonicalHeaders);
        // SignedHeaders 签名头域
        String signedHeaders = "host;x-bce-date";
        // SigningKey 派生密钥
        String signingKey = hMacSha256(authStringPrefix, SK);
        System.out.println("SigningKey = " + signingKey);
        // Signature 签名摘要
        String signature = hMacSha256(canonicalRequest, signingKey);
        System.out.println("Signature = " + signature);

        // ### Authorization 认证字符串 ###
        String authorization = authStringPrefix + "/" + signedHeaders + "/" + signature;
        System.out.println("Authorization = " + authorization);

        sendPost("http://bcd.baidubce.com/v1/domain/resolve/list", "{\n\"domain\" : \"stackoverflow.wiki\"\n}", timestamp, authorization);
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
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] array = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte item : array) {
            sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
        }

        return sb.toString();
    }

    public static String sendPost(String url, String param, String time, String authorization) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("Host", "bcd.baidubce.com");
            conn.setRequestProperty("x-bce-date", time);
            conn.setRequestProperty("Authorization", authorization);
            conn.setRequestProperty("User-Agent", "");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static Date parseUTCText(String text) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (text.indexOf(".") > -1) {
            String prefix = text.substring(0, text.indexOf("."));
            String suffix = text.substring(text.indexOf("."));
            if (suffix.length() >= 5) {
                suffix = suffix.substring(0, 4) + "Z";
            } else {
                int len = 5 - suffix.length();
                String temp = "";
                temp += suffix.substring(0, suffix.length() - 1);
                for (int i = 0; i < len; i++) {
                    temp += "0";
                }
                suffix = temp + "Z";
            }
            text = prefix + suffix;
        } else {
            text = text.substring(0, text.length() - 1) + ".000Z";
        }
        Date date = sdf.parse(text);
        return date;
    }
}
