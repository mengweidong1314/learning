# 🚀 PDF下载系统快速启动指南

## 📦 一键启动

```bash
# 1. 启动应用
mvn spring-boot:run

# 2. 在新终端窗口中运行测试脚本
./test-pdf-download.sh
```

## 🌐 访问地址

- **Web界面**: http://localhost:8080
- **H2数据库控制台**: http://localhost:8080/h2-console
- **API基础路径**: http://localhost:8080/api/refunds

## 🔧 H2数据库连接信息

```
JDBC URL: jdbc:h2:mem:testdb
User Name: sa
Password: password
```

## 📋 主要功能演示

### 1. 单个退款申请PDF下载
```bash
curl -o "退款申请.pdf" "http://localhost:8080/api/refunds/1/pdf"
```

### 2. 退款申请列表PDF下载
```bash
curl -o "退款申请列表.pdf" "http://localhost:8080/api/refunds/list/pdf"
```

### 3. 统计报告PDF下载
```bash
curl -o "统计报告.pdf" "http://localhost:8080/api/refunds/statistics/pdf"
```

### 4. 按状态筛选PDF下载
```bash
# 待审核状态 (status=0)
curl -o "待审核.pdf" "http://localhost:8080/api/refunds/list/pdf?status=0"

# 已通过状态 (status=1)
curl -o "已通过.pdf" "http://localhost:8080/api/refunds/list/pdf?status=1"

# 已拒绝状态 (status=2)
curl -o "已拒绝.pdf" "http://localhost:8080/api/refunds/list/pdf?status=2"
```

### 5. 按时间范围筛选PDF下载
```bash
curl -o "时间范围.pdf" "http://localhost:8080/api/refunds/list/pdf?startTime=2024-01-01%2000:00:00&endTime=2024-12-31%2023:59:59"
```

### 6. 自定义内容PDF生成
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"title":"自定义标题","content":"自定义内容\n支持多行文本"}' \
  -o "自定义.pdf" \
  "http://localhost:8080/api/refunds/custom/pdf"
```

## 🎯 Web界面功能

访问 http://localhost:8080 可以使用Web界面：

1. **查看所有退款申请**
2. **按条件筛选**
3. **一键下载各种PDF**
4. **查看详细信息**

## 📊 示例数据

系统启动时会自动创建以下测试数据：

- 5个不同状态的退款申请
- 不同金额和时间的申请记录
- 包含完整的申请人信息

## 🔍 数据结构

退款申请包含以下字段：
- 申请单号、申请人姓名、手机号
- 原订单号、退款金额、退款原因
- 银行卡号、开户行信息
- 申请时间、处理时间、状态等

## 📱 移动端适配

Web界面已适配移动端，可在手机浏览器中正常使用。

## 🛠️ 技术特点

- ✅ **中文字体支持**：PDF完美显示中文
- ✅ **数据脱敏**：自动处理手机号、银行卡号
- ✅ **响应式设计**：Web界面适配各种屏幕
- ✅ **RESTful API**：标准化接口设计
- ✅ **内存数据库**：无需额外配置

## 🚨 注意事项

1. 确保已安装JDK 17或更高版本
2. 确保Maven已正确安装
3. 端口8080不被其他程序占用
4. 下载的PDF文件会保存在当前目录