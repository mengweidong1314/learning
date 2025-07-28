# PDF下载演示系统 - 退款申请

一个完整的PDF下载功能演示项目，基于Spring Boot开发，以退款申请为业务场景，展示了各种PDF生成和下载的实现方式。

## 项目特点

- ✅ **完整的PDF生成功能**：使用iText 7生成专业的PDF文档
- ✅ **多种下载方式**：支持单个文档、列表、统计报告等多种PDF下载
- ✅ **中文支持**：完美支持中文字体显示
- ✅ **RESTful API**：标准的REST接口设计
- ✅ **Web界面**：提供友好的测试界面
- ✅ **数据脱敏**：自动处理敏感信息（手机号、银行卡号等）
- ✅ **灵活查询**：支持按状态、时间范围等条件筛选

## 技术栈

- **后端框架**：Spring Boot 3.2.1
- **数据库**：H2 Database（内存数据库，便于演示）
- **PDF生成**：iText 7.2.5 + 中文字体支持
- **ORM框架**：Spring Data JPA
- **构建工具**：Maven
- **Java版本**：JDK 17

## 项目结构

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── PdfDownloadApplication.java      # 主启动类
│   │   ├── controller/
│   │   │   └── RefundController.java        # REST控制器
│   │   ├── entity/
│   │   │   └── RefundApplication.java       # 退款申请实体
│   │   ├── repository/
│   │   │   └── RefundApplicationRepository.java # 数据访问层
│   │   └── service/
│   │       ├── PdfService.java              # PDF服务接口
│   │       ├── RefundApplicationService.java # 业务服务接口
│   │       └── impl/
│   │           ├── PdfServiceImpl.java      # PDF服务实现
│   │           └── RefundApplicationServiceImpl.java # 业务服务实现
│   └── resources/
│       ├── application.yml                  # 配置文件
│       └── static/
│           └── index.html                   # 测试界面
└── pom.xml                                 # Maven配置
```

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本

### 2. 运行项目

```bash
# 克隆项目
git clone <repository-url>
cd pdf-download-demo

# 编译并运行
mvn clean spring-boot:run
```

### 3. 访问系统

- **Web界面**：http://localhost:8080
- **API文档**：http://localhost:8080/api/refunds/help
- **H2控制台**：http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - 用户名: `sa`
  - 密码: `password`

## API接口文档

### 基础接口

| 方法 | URL | 描述 |
|------|-----|------|
| GET | `/api/refunds` | 获取所有退款申请 |
| GET | `/api/refunds/{id}` | 获取单个退款申请 |
| POST | `/api/refunds` | 创建退款申请 |
| PUT | `/api/refunds/{id}/review` | 审核退款申请 |
| POST | `/api/refunds/create-sample` | 创建示例数据 |
| GET | `/api/refunds/help` | 获取API使用说明 |

### PDF下载接口

| 方法 | URL | 描述 |
|------|-----|------|
| GET | `/api/refunds/{id}/download/pdf` | 下载单个退款申请PDF |
| GET | `/api/refunds/download/list-pdf` | 下载完整申请列表PDF |
| GET | `/api/refunds/download/list-pdf-by-status?status=1` | 按状态下载PDF |
| GET | `/api/refunds/download/list-pdf-by-date?startTime=...&endTime=...` | 按时间范围下载PDF |
| GET | `/api/refunds/download/statistics-pdf` | 下载统计报告PDF |
| POST | `/api/refunds/download/custom-pdf` | 生成自定义PDF |

## 使用示例

### 1. 创建示例数据

```bash
curl -X POST http://localhost:8080/api/refunds/create-sample
```

### 2. 下载单个申请PDF

```bash
curl -O -J http://localhost:8080/api/refunds/1/download/pdf
```

### 3. 下载申请列表PDF

```bash
curl -O -J http://localhost:8080/api/refunds/download/list-pdf
```

### 4. 按状态下载PDF

```bash
# 下载所有"审核通过"状态的申请
curl -O -J "http://localhost:8080/api/refunds/download/list-pdf-by-status?status=1"
```

### 5. 按时间范围下载PDF

```bash
curl -O -J "http://localhost:8080/api/refunds/download/list-pdf-by-date?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59"
```

### 6. 下载统计报告PDF

```bash
curl -O -J http://localhost:8080/api/refunds/download/statistics-pdf
```

### 7. 生成自定义PDF

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"title":"自定义报告","content":"这是自定义内容"}' \
  -O -J \
  http://localhost:8080/api/refunds/download/custom-pdf
```

## 状态码说明

| 状态码 | 状态名称 | 描述 |
|--------|----------|------|
| 0 | 待审核 | 申请已提交，等待审核 |
| 1 | 审核通过 | 申请已通过审核，准备退款 |
| 2 | 审核拒绝 | 申请被拒绝 |
| 3 | 退款中 | 正在处理退款 |
| 4 | 退款完成 | 退款已完成 |
| 5 | 退款失败 | 退款处理失败 |

## 主要功能特性

### 1. PDF生成功能

- **单个申请PDF**：包含完整的申请信息，格式化布局
- **申请列表PDF**：表格形式展示多个申请，支持分页
- **统计报告PDF**：包含状态统计、图表分析
- **自定义PDF**：支持用户自定义标题和内容

### 2. 数据安全

- **手机号脱敏**：`138****1234`
- **银行卡号脱敏**：`6222****8888`
- **字段验证**：输入数据校验
- **异常处理**：完善的错误处理机制

### 3. 查询功能

- **按状态查询**：筛选特定状态的申请
- **按时间范围查询**：支持日期时间范围筛选
- **分页查询**：大数据量的分页处理
- **统计查询**：各种统计分析

### 4. 文件处理

- **中文支持**：完美支持中文字体
- **文件命名**：智能的文件命名规则
- **下载优化**：支持浏览器直接下载
- **格式控制**：专业的PDF格式布局

## 扩展功能

### 1. 添加新的PDF模板

可以在 `PdfService` 中添加新的PDF生成方法：

```java
public interface PdfService {
    // 添加新的PDF生成方法
    byte[] generateCustomReportPdf(CustomReportData data);
}
```

### 2. 支持其他文件格式

可以扩展支持Excel、Word等格式：

```java
// 添加Excel导出功能
byte[] generateRefundApplicationExcel(List<RefundApplication> applications);
```

### 3. 添加邮件发送功能

可以结合邮件服务，直接发送PDF到指定邮箱：

```java
void sendPdfByEmail(String email, byte[] pdfBytes, String filename);
```

## 常见问题

### Q: 如何修改PDF字体？

A: 在 `PdfServiceImpl` 中修改 `FONT_PATH` 常量，指向你需要的字体文件。

### Q: 如何自定义PDF样式？

A: 修改 `PdfServiceImpl` 中的样式相关代码，包括字体大小、颜色、布局等。

### Q: 如何增加新的数据字段？

A: 
1. 在 `RefundApplication` 实体中添加新字段
2. 在 `PdfServiceImpl` 中更新PDF生成逻辑
3. 在前端界面中添加对应的显示

### Q: 如何部署到生产环境？

A: 
1. 修改 `application.yml` 中的数据库配置
2. 打包：`mvn clean package`
3. 运行：`java -jar target/pdf-download-demo-1.0.0.jar`

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交Issue或联系开发者。

---

**注意**：本项目仅用于学习和演示目的，生产环境使用前请进行充分测试和安全评估。
