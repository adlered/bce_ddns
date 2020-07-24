package pers.adlered.bce.ddns;

import com.google.gson.Gson;
import pers.adlered.bce.ddns.bean.list.DnsList;
import pers.adlered.bce.ddns.bean.list.DnsListInfo;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static String AK;
    private static String SK;

    private static long nextTimeMillis = 10 * 60 * 1000;

    /**
     * java -jar bce_ddns.jar [Domain] [A Record] [AK] [SK] [Interval By Minute (Optional)]
     *
     * @param args 参数
     */
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            try {
                String userDomain;
                String userRecord;
                try {
                    userDomain = args[0];
                    userRecord = args[1];
                    AK = args[2];
                    SK = args[3];
                } catch (Exception e) {
                    System.out.println("Usage: java -jar bce_ddns.jar [Domain] [A Record] [AK] [SK] [Interval By Minute (Optional)]");
                    break;
                }
                try {
                    nextTimeMillis = Long.parseLong(args[4]) * 60 * 1000;
                } catch (Exception e) {
                    nextTimeMillis = 10 * 60 * 1000;
                }

                String listParam = "{\n" +
                        "\"domain\" : \"" + userDomain + "\"\n" +
                        "}";
                String domainResult = run("list", listParam);
                Gson gson = new Gson();
                DnsList dnsList = gson.fromJson(domainResult, DnsList.class);
                // 如果 recordExists 为 false，会新建一个 A 记录，反之更新
                boolean recordExists = false;
                int recordId = 0;
                for (DnsListInfo dnsInfo : dnsList.result) {
                    String record = dnsInfo.domain;
                    String type = dnsInfo.rdtype;
                    if (userRecord.equals(record) && "A".equals(type)) {
                        recordExists = true;
                        recordId = dnsInfo.recordId;
                        System.out.println("Found exists A-Record: " + type + " " + record);
                    }
                }
                if (recordExists) {
                    String updateParam = "{\n" +
                            "\"recordId\" : " + recordId + ",\n" +
                            "\"domain\" : \"" + userRecord + "\",\n" +
                            "\"rdType\" : \"A\",\n" +
                            "\"rdata\" : \"" + getRealIp() + "\",\n" +
                            "\"zoneName\" : \"" + userDomain + "\"\n" +
                            "}";
                    System.out.println("Updating A-Record...\n" + updateParam);
                    System.out.println(run("edit", updateParam));
                } else {
                    String addParam = "{\n" +
                            "\"domain\" : \"" + userRecord + "\",\n" +
                            "\"rdType\" : \"A\",\n" +
                            "\"rdata\" : \"" + getRealIp() + "\",\n" +
                            "\"zoneName\" : \"" + userDomain + "\"\n" +
                            "}";
                    System.out.println("Newing A-Record...\n" + addParam);
                    System.out.println(run("add", addParam));
                }
            } catch (Exception e) {
                System.out.println("An error has been captured: " + e.getMessage());
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
            String now = dateFormat.format(new Date());
            String next = dateFormat.format(new Date(System.currentTimeMillis() + nextTimeMillis));
            System.out.println("Done, waiting for next time [nowTime=" + now + ", nextRuntime=" + next + "]");
            Thread.sleep(nextTimeMillis);
        }
    }

    /**
     * 请求域名解析相关接口
     *
     * @param mode  支持三个 API：
     *              list - 列表
     *              add - 添加
     *              edit - 编辑
     * @param param 参数
     * @return 返回 JSON
     * @throws Exception 异常处理丢给主线程
     */
    private static String run(String mode, String param) throws Exception {
        authStringPrefix = "bce-auth-v1/{accessKeyId}/{timestamp}/{expirationPeriodInSeconds}";
        canonicalRequest = "{httpMethod}" + "\n" + "{canonicalURI}" + "\n" + "{canonicalQueryString}" + "\n" + "{canonicalHeaders}";
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
        String queryUri = "/v1/domain/resolve/" + mode;
        String canonicalUri = URLEncoder.encode(queryUri, "UTF-8");
        canonicalUri = canonicalUri.replaceAll("%2F", "/");
        setCanonicalRequest("canonicalURI", canonicalUri);
        // CanonicalQueryString
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

        String result = sendPost("http://bcd.baidubce.com/v1/domain/resolve/" + mode, param, timestamp, authorization);
        System.out.println("HTTP 200 OK");

        return result;
    }

    private static void setAuthStringPrefix(String key, Object value) {
        authStringPrefix = authStringPrefix.replaceAll(String.format("\\{%s\\}", key), value.toString());
    }

    private static void setCanonicalRequest(String key, Object value) {
        canonicalRequest = canonicalRequest.replaceAll(String.format("\\{%s\\}", key), value.toString());
    }

    public static String hMacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] array = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte item : array) {
            sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
        }

        return sb.toString();
    }

    public static String sendPost(String url, String param, String time, String authorization) throws Exception {
        PrintWriter out;
        BufferedReader in;
        StringBuilder result = new StringBuilder();
        URL realUrl = new URL(url);
        // 打开和 URL 之间的连接
        URLConnection conn = realUrl.openConnection();
        // 设置通用的请求属性
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Host", "bcd.baidubce.com");
        conn.setRequestProperty("x-bce-date", time);
        conn.setRequestProperty("Authorization", authorization);
        // 发送 POST 请求必须设置如下两行
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // 获取 URLConnection 对象对应的输出流
        out = new PrintWriter(conn.getOutputStream());
        // 发送请求参数
        out.print(param);
        // Flush 输出流的缓冲
        out.flush();
        // 定义 BufferedReader 输入流来读取URL的响应
        in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            result.append(line);
        }
        // 关闭输出流、输入流
        out.close();
        in.close();

        return result.toString();
    }
    
    private static String getRealIp() {
        String ip = httpGet("https://programmingwithlove.stackoverflow.wiki/ip");
        System.out.println("[GETIP] " + ip);
        if (ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }

    private static String httpGet(String url) {
        StringBuffer buffer = new StringBuffer();
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36");
            conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            try (InputStream inputStream = conn.getInputStream();
                 InputStreamReader streamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(streamReader)) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
}
