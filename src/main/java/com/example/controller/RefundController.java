package com.example.controller;

import com.example.entity.RefundApplication;
import com.example.service.PdfService;
import com.example.service.RefundApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 退款申请控制器 - 提供PDF下载功能
 * 
 * @author AI Assistant
 */
@Slf4j
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundApplicationService refundApplicationService;
    private final PdfService pdfService;

    /**
     * 获取所有退款申请列表
     */
    @GetMapping
    public ResponseEntity<List<RefundApplication>> getAllRefunds() {
        List<RefundApplication> refunds = refundApplicationService.findAll();
        return ResponseEntity.ok(refunds);
    }

    /**
     * 根据ID获取退款申请详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<RefundApplication> getRefundById(@PathVariable Long id) {
        Optional<RefundApplication> refund = refundApplicationService.findById(id);
        return refund.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建新的退款申请
     */
    @PostMapping
    public ResponseEntity<RefundApplication> createRefund(@RequestBody RefundApplication refundApplication) {
        try {
            RefundApplication created = refundApplicationService.createRefundApplication(refundApplication);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("创建退款申请失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 审核退款申请
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<RefundApplication> reviewRefund(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String reviewer,
            @RequestParam(required = false) String remark) {
        try {
            RefundApplication reviewed = refundApplicationService.reviewRefundApplication(id, status, reviewer, remark);
            return ResponseEntity.ok(reviewed);
        } catch (Exception e) {
            log.error("审核退款申请失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载单个退款申请PDF
     * URL示例: GET /api/refunds/1/download/pdf
     */
    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<byte[]> downloadRefundApplicationPdf(@PathVariable Long id) {
        try {
            Optional<RefundApplication> optionalRefund = refundApplicationService.findById(id);
            if (optionalRefund.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            RefundApplication refund = optionalRefund.get();
            byte[] pdfBytes = pdfService.generateRefundApplicationPdf(refund);

            String filename = "退款申请单_" + refund.getApplicationNo() + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache");

            log.info("生成退款申请PDF成功，申请单号: {}, 文件大小: {} bytes", 
                refund.getApplicationNo(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("下载退款申请PDF失败，ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载退款申请列表PDF（全部）
     * URL示例: GET /api/refunds/download/list-pdf
     */
    @GetMapping("/download/list-pdf")
    public ResponseEntity<byte[]> downloadRefundListPdf() {
        try {
            List<RefundApplication> refunds = refundApplicationService.findAll();
            if (refunds.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            byte[] pdfBytes = pdfService.generateRefundApplicationListPdf(refunds);

            String filename = "退款申请列表_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache");

            log.info("生成退款申请列表PDF成功，记录数: {}, 文件大小: {} bytes", 
                refunds.size(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("下载退款申请列表PDF失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 按状态下载退款申请列表PDF
     * URL示例: GET /api/refunds/download/list-pdf?status=1
     */
    @GetMapping("/download/list-pdf-by-status")
    public ResponseEntity<byte[]> downloadRefundListPdfByStatus(@RequestParam Integer status) {
        try {
            List<RefundApplication> refunds = refundApplicationService.findByStatus(status);
            if (refunds.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            byte[] pdfBytes = pdfService.generateRefundApplicationListPdf(refunds);

            String statusDesc = getStatusDescription(status);
            String filename = "退款申请列表_" + statusDesc + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache");

            log.info("生成{}状态退款申请列表PDF成功，记录数: {}, 文件大小: {} bytes", 
                statusDesc, refunds.size(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("按状态下载退款申请列表PDF失败，status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 按时间范围下载退款申请列表PDF
     * URL示例: GET /api/refunds/download/list-pdf-by-date?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59
     */
    @GetMapping("/download/list-pdf-by-date")
    public ResponseEntity<byte[]> downloadRefundListPdfByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<RefundApplication> refunds = refundApplicationService.findByApplicationTimeBetween(startTime, endTime);
            if (refunds.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            byte[] pdfBytes = pdfService.generateRefundApplicationListPdf(refunds);

            String filename = "退款申请列表_" + 
                startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "至" +
                endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache");

            log.info("生成时间范围退款申请列表PDF成功，时间范围: {} 至 {}, 记录数: {}, 文件大小: {} bytes", 
                startTime, endTime, refunds.size(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("按时间范围下载退款申请列表PDF失败，startTime: {}, endTime: {}", startTime, endTime, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载退款申请统计报告PDF
     * URL示例: GET /api/refunds/download/statistics-pdf
     */
    @GetMapping("/download/statistics-pdf")
    public ResponseEntity<byte[]> downloadRefundStatisticsPdf() {
        try {
            List<Object[]> statusCounts = refundApplicationService.getStatusStatistics();
            if (statusCounts.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            byte[] pdfBytes = pdfService.generateRefundStatisticsPdf(statusCounts);

            String filename = "退款申请统计报告_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache");

            log.info("生成退款申请统计报告PDF成功，统计项数: {}, 文件大小: {} bytes", 
                statusCounts.size(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("下载退款申请统计报告PDF失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载自定义PDF文档
     * URL示例: POST /api/refunds/download/custom-pdf
     * Body: {"title": "自定义标题", "content": "自定义内容"}
     */
    @PostMapping("/download/custom-pdf")
    public ResponseEntity<byte[]> downloadCustomPdf(@RequestBody CustomPdfRequest request) {
        try {
            byte[] pdfBytes = pdfService.generateCustomPdf(request.getTitle(), request.getContent());

            String filename = (request.getTitle() != null ? request.getTitle() : "自定义文档") + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache");

            log.info("生成自定义PDF成功，标题: {}, 文件大小: {} bytes", 
                request.getTitle(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("下载自定义PDF失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 创建示例退款申请（用于测试）
     */
    @PostMapping("/create-sample")
    public ResponseEntity<RefundApplication> createSampleRefund() {
        try {
            RefundApplication sample = RefundApplication.builder()
                    .applicantName("测试用户")
                    .applicantPhone("13800138000")
                    .applicantEmail("test@example.com")
                    .originalOrderNo("ORD" + System.currentTimeMillis())
                    .refundAmount(new BigDecimal("299.00"))
                    .refundReason("测试退款申请")
                    .refundType(1)
                    .status(0)
                    .bankCardNo("6222888888888888")
                    .bankName("测试银行")
                    .accountHolder("测试用户")
                    .build();

            RefundApplication created = refundApplicationService.createRefundApplication(sample);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (Exception e) {
            log.error("创建示例退款申请失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取API使用说明
     */
    @GetMapping("/help")
    public ResponseEntity<String> getApiHelp() {
        StringBuilder help = new StringBuilder();
        help.append("退款申请PDF下载API使用说明：\n\n");
        help.append("1. 获取所有申请: GET /api/refunds\n");
        help.append("2. 获取单个申请: GET /api/refunds/{id}\n");
        help.append("3. 创建申请: POST /api/refunds\n");
        help.append("4. 下载单个申请PDF: GET /api/refunds/{id}/download/pdf\n");
        help.append("5. 下载申请列表PDF: GET /api/refunds/download/list-pdf\n");
        help.append("6. 按状态下载PDF: GET /api/refunds/download/list-pdf-by-status?status=1\n");
        help.append("7. 按时间范围下载PDF: GET /api/refunds/download/list-pdf-by-date?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59\n");
        help.append("8. 下载统计报告PDF: GET /api/refunds/download/statistics-pdf\n");
        help.append("9. 下载自定义PDF: POST /api/refunds/download/custom-pdf\n");
        help.append("10. 创建示例数据: POST /api/refunds/create-sample\n\n");
        help.append("状态码说明: 0-待审核, 1-审核通过, 2-审核拒绝, 3-退款中, 4-退款完成, 5-退款失败\n");

        return ResponseEntity.ok(help.toString());
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "审核拒绝";
            case 3 -> "退款中";
            case 4 -> "退款完成";
            case 5 -> "退款失败";
            default -> "未知状态";
        };
    }

    /**
     * 自定义PDF请求DTO
     */
    public static class CustomPdfRequest {
        private String title;
        private String content;

        public CustomPdfRequest() {}

        public CustomPdfRequest(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}