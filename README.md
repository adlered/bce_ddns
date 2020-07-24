# 百度云域名解析 DDNS

> 百度云官方没有提供域名解析的 SDK，所以照着文档写了很久，终于完成了这个百度云 DDNS 工具，自己写了个 GetIp 的服务端接口，无限制使用，稳定好用，喜欢的请给个 Star :)

### 使用方法

到 [Release页面](https://github.com/adlered/bce_ddns/releases) 下载打包好的 jar 包，使用以下命令运行（需要先安装 Java 并且配置好环境变量）：

```
java -jar bce_ddns.jar [域名] [A 记录] [Access Key] [Secret Key] [循环更新时间，按分钟间隔（可选，默认10分钟）]
```

1. 在百度云提交工单申请开启域名解析 API 使用权限
2. 在右上角-安全认证生成 `Access Key` 和 `Secret Key`
3. 假如我要将我的域名 `myhome.stackoverflow.wiki` 进行DDNS定时更新，命令示例如下：

```
java -jar bce_ddns.jar stackoverflow.wiki myhome 你的AccessKey 你的SecretKey
```

### 在后台运行

如果要在后台驻留运行，可以参考以下方法：

```
Windows：
javaw -jar bce_ddns.jar [参数]

Linux/MacOS：
java -jar bce_ddns.jar [参数] &
或者
nohup java -jar bce_ddns.jar [参数] &
```