### 概述
- 类filebeat的轻量级日志采集工具，java编写，可运行在aix/linux等机器上，最低支持java5。相对filebeat，增加了事务合并功能（即不连续的行通过关联id可以组合成一行）。

### 参数配置
- output.logstash.net_max: 限流比特数，0为不限流，默认为0
- output.logstash.hosts: logstash或者jlogstash的ip端口的列表，列表元素格式如 localhost:8635
- logging.to_files: 是否输出到文件，默认为true。false会输出到标准输出
- logging.level: 日志级别，默认为info
- logging.files.keepfiles: 日志文件保留个数
- logging.files.path: 文件路径
- logging.files.name: 文件名称
- logging.files.max_size: 文件大小阈值
- filebeat.idle_timeout: 监听文件变化的时间间隔，默认1s
- filebeat.buffer_size: 缓存一行日志的最大字节数，默认为1024*1024，即1MB
- filebeat.network_timeout: 网络连接超时时间，默认60s
- filebeat.prospectors[x].tail_files: 是否从最近的日志开始读起，默认为true。
- filebeat.prospectors[x].paths: 监听的文件路径，支持模糊匹配及递归匹配，格式如:/home/user/*/*.log
- filebeat.prospectors[x].encoding: 编码，默认为utf-8
- filebeat.prospectors[x].exclude_files: 排除的文件，支持正则
- filebeat.prospectors[x].exclude_lines: 排除的行，支持正则
- filebeat.prospectors[x].include_lines: 包含的行，支持正则
- filebeat.prospectors[x].fields：填写键值对，键值对会和消息一起发送给logstash/jlogstash，其中keeptype、logtype、appname、tag、uesr_token、uuid是必带参数。keeptype一般填business，用于标志日志保留时间；appname用于分类日志的；tag用于更细粒度的日志分类的；user_token是租户信息的加密串；uuid是相当于jfilebeat的唯一id。
- filebeat.prospectors[x].multiline.negate: pattern匹配结果的取反值，默认为true，一般的场景使用默认值即可。
- filebeat.prospectors[x].multiline.pattern: 匹配语句，支持正则。
- filebeat.prospectors[x].multiline.match: 可填before和after，默认为after。after可以理解成匹配首行，before则是匹配末行。
- filebeat.prospectors[x].multiline.timeout: 在指定时间内没完成匹配，将触发超时，直接输出日志，不再匹配，默认为10s。
- filebeat.prospectors[x].multiline.max_lines: 在指定时间内部匹配将触发超时，直接输出日志，不再匹配，默认为10s。
- filebeat.prospectors[x].transactionline.patterns: 事务合并匹配语句的列表。列表元素格式如 (?^\w+).*begin -> ^${name} -> .. -> ^${name}.*end。 意思是从包含begin的行到包含end的行，且都包含name变量的行会合并成一个事务日志。 其中，->是事务向量，连接事务上下规则，规则支持正则匹配，(?)为捕获功能，捕获的key可以在下游规则使用，使用是采取${}获取变量。..表示无数次沿用上一规则，即^${name}。 ^${name}.*end属于终结规则，他会先于..进行匹配，一旦匹配终结规则，事务合并结束，不再进行..匹配。注意->前后必须包含空格，否则当做正则本身处理。
- filebeat.prospectors[x].transactionline.timeout：事务合并的超时时间，类似multiline的timeout。
- filebeat.prospectors[x].transactionline.max_lines：事务合并的行数上限，类似multiline的max_lines。
- filebeat.prospectors[x].transactionline.end_flag：可填include和exclude，默认include。include表示终结规则匹配的行会包含在事务当中，exclude则相反，不包含在事务中。

### 日志合并事例
#### 日志1
```
[2018-04-07 09:25:09] INFO   - 交易[0426]--<上送报文>------------组包开始-----
[2018-04-07 09:30:39] INFO   - [_ZKH],值=[622369**********]
[2018-04-07 09:25:09] INFO   - [_errmsg],值=[交易成功]
[2018-04-07 09:25:09] INFO   - [_hostcode],值=[0000]
[2018-04-07 09:25:09] INFO   - --<下传报文>------------解包结束-----
[2018-04-07 09:25:09] INFO   - 与主机通信耗时:[210]ms
[2018-04-07 09:25609] INFO   - 交易[0617]--<上送报文>------------组包开始-----
```
日志连续，没有线程并发写入，而且有明确的终结语句"与主机通信耗时"，那么只需要用多行合并就行。“与主机通信耗时”是处于末尾行，所以match要填before，pattern的话填“与主机通信耗时”，nagate一律取true即可。timeout和max_lines可以尽量长点，确保最终匹配到"与主机通信耗时"，所以匹配规则写成

```
multiline:
      negate: true
      pattern: '与主机通信耗时'
      match: "before"
      timeout: "1800s"
      max_lines: 1500
```

#### 日志2
```
0502:155243:481|T1234|L5|routeIn.cpp:289|转发交易请求[WFM:Ncs2pl:ncs2AcctValid]  
上传数据:
T1234/名字空间::
T1234/  域名|类型|长度|数据值
T1234/DEFAULT::
T1234/  A162|S|4|0.00
0502:155243:483|T1234|L8|COrbCli.cpp:814|Send to server: 
0502:155244:245|T1234|L8|COrbCli.cpp:861|Server response:
T1234/名字空间::
T1234/  域名|类型|长度|数据值
T1234/DEFAULT::
T1234/  C180|S|18|201805020068913050
T1234/  C601|S|8|验证成功
T1234/  I010|S|1|1
T1234/  S100|S|0|
T1234/  WFMCode|I|1|0
T1234/  WFMMsg|S|7|Success
T1234/  _errmsg|S|8|交易成功
T1234/  _hostcode|S|4|9***
```
有多线程并发写，日志不连续，而且逻辑行是多行(如，首行以0502:155243，知道遇到写一个相同格式的行结束)，需求是合并上述从"转发交易请求"直到"Server response"的同一线程的行。这时候就需要使用多行合并，先合并逻辑行，逻辑行都是以四个数字加冒号开始的，所以pattern写成^\d{4}:，由于是首行，所以match为after，nagate继续选true，timeout和max_lines选尽量大的值即可，所以多行合并部分写成：

```
multiline:
  negate: true
  pattern: '^\d{4}:'
  match: "after"
  timeout: "1800s"
  max_lines: 5000
```

如果只配置了多行合并，没有配置事务合并，那么上述日志会被分割成3行：
<br>（1）

```
0502:155243:481|T1234|L5|routeIn.cpp:289|转发交易请求[WFM:Ncs2pl:ncs2AcctValid]  
上传数据:
T1234/名字空间::
T1234/  域名|类型|长度|数据值
T1234/DEFAULT::
T1234/  A162|S|4|0.00
```

（2）

```
0502:155243:483|T1234|L8|COrbCli.cpp:814|Send to server: 
```

（3）

```
0502:155244:245|T1234|L8|COrbCli.cpp:861|Server response:
T1234/名字空间::
T1234/  域名|类型|长度|数据值
T1234/DEFAULT::
T1234/  C180|S|18|201805020068913050
T1234/  C601|S|8|验证成功
T1234/  I010|S|1|1
T1234/  S100|S|0|
T1234/  WFMCode|I|1|0
T1234/  WFMMsg|S|7|Success
T1234/  _errmsg|S|8|交易成功
T1234/  _hostcode|S|4|9***
```

如果要把1、2、3合并成一行，这时候就要再进行事务合并，合并成事务行，事务行可以跳行合并多行（如1和2之前的其他线程留下的行会被跳过）。步骤如下：

1. 确定开始规则。行以包含"转发交易请求"字眼开始，所以开始规则写成: 转发交易请求
2. 确定终结规则。行包含"Server response"字眼，所以终结规则写成：Server\s+response
3. 确定中间规则。行包含与开始规则相同的线程号，这时候怎么办呢？这就需要用(?<key>value)把上游的线程号捕获并下传下来，再在中间规则中写上${key}来使用上游的key变量，所以中间规则改成: ${thread}。但注意中间规则是可能需要匹配多个行的
4. 修改开始规则。由于步骤3的原因，需要在规则当中用(?<thread>T\d+)捕获线程号，而线程号之前的字符串“0502:155243:481|”则用^\d+:\d+:\d+\|匹配，所以开始规则写成：^\d+:\d+:\d+\|(?<thread>T\d+).*转发交易请求
5. 修改终结规则。由于要和步骤4的开始规则用同一线程号，所以要改成：${thread}.*Server\s+response
6. 用->连接3、4、5的规则，则pattern为^\d+:\d+:\d+\|(?<thread>T\d+).*转发交易请求 -> ${thread} -> .. -> ${thread}.*Server\s+response。注：由于pattern中含有特殊字符，写在yaml文件里面推荐用单引号括起pattern，不用双引号是因为要额外对特殊字符转义。

最终的配置成：

```
multiline:
  negate: true
  pattern: '^\d{4}:'
  match: "after"
  timeout: "1800s"
  max_lines: 5000
transactionline:
  patterns:
    - '^\d+:\d+:\d+\|(?<thread>T\d+).*转发交易请求 -> ${thread} -> .. -> ${thread}.*Server\s+response'
  timeout: "60s"
  max_lines: 1000
```

