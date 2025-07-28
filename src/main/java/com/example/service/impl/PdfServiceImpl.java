package com.example.service.impl;

import com.example.entity.RefundApplication;
import com.example.service.PdfService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF生成服务实现类
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
public class PdfServiceImpl implements PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FONT_PATH = "STSong-Light";

    @Override
    public byte[] generateRefundApplicationPdf(RefundApplication refundApplication) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            // 设置中文字体
            PdfFont font = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            document.setFont(font);

            // 添加标题
            Paragraph title = new Paragraph("退款申请单")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // 创建主表格
            Table mainTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));

            // 基本信息
            addTableRow(mainTable, "申请单号", refundApplication.getApplicationNo());
            addTableRow(mainTable, "申请人姓名", refundApplication.getApplicantName());
            addTableRow(mainTable, "申请人手机", refundApplication.getApplicantPhone());
            addTableRow(mainTable, "申请人邮箱", refundApplication.getApplicantEmail());
            addTableRow(mainTable, "原订单号", refundApplication.getOriginalOrderNo());
            addTableRow(mainTable, "退款金额", "¥" + refundApplication.getRefundAmount().toString());
            addTableRow(mainTable, "退款类型", refundApplication.getRefundTypeDescription());
            addTableRow(mainTable, "申请状态", refundApplication.getStatusDescription());
            addTableRow(mainTable, "申请时间", 
                refundApplication.getApplicationTime().format(DATE_FORMATTER));

            // 退款原因
            addTableRow(mainTable, "退款原因", refundApplication.getRefundReason());

            // 银行信息
            if (refundApplication.getBankCardNo() != null) {
                addTableRow(mainTable, "银行卡号", maskBankCardNo(refundApplication.getBankCardNo()));
                addTableRow(mainTable, "开户银行", refundApplication.getBankName());
                addTableRow(mainTable, "开户人", refundApplication.getAccountHolder());
            }

            // 审核信息
            if (refundApplication.getReviewTime() != null) {
                addTableRow(mainTable, "审核时间", 
                    refundApplication.getReviewTime().format(DATE_FORMATTER));
                addTableRow(mainTable, "审核人", refundApplication.getReviewer());
                if (refundApplication.getReviewRemark() != null) {
                    addTableRow(mainTable, "审核备注", refundApplication.getReviewRemark());
                }
            }

            // 退款完成信息
            if (refundApplication.getRefundTime() != null) {
                addTableRow(mainTable, "退款完成时间", 
                    refundApplication.getRefundTime().format(DATE_FORMATTER));
            }

            document.add(mainTable);

            // 添加页脚
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("生成退款申请PDF失败", e);
            throw new RuntimeException("生成PDF失败", e);
        }
    }

    @Override
    public byte[] generateRefundApplicationListPdf(List<RefundApplication> refundApplications) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate()); // 横向页面

            // 设置中文字体
            PdfFont font = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            document.setFont(font);

            // 添加标题
            Paragraph title = new Paragraph("退款申请列表")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // 添加统计信息
            Paragraph summary = new Paragraph("总计：" + refundApplications.size() + " 条记录")
                    .setFontSize(12)
                    .setMarginBottom(10);
            document.add(summary);

            // 创建表格
            Table table = new Table(UnitValue.createPercentArray(new float[]{8, 12, 10, 10, 15, 8, 12, 10, 15}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setFontSize(8);

            // 添加表头
            addTableHeader(table, "申请单号");
            addTableHeader(table, "申请人");
            addTableHeader(table, "手机号");
            addTableHeader(table, "订单号");
            addTableHeader(table, "退款金额");
            addTableHeader(table, "类型");
            addTableHeader(table, "状态");
            addTableHeader(table, "申请时间");
            addTableHeader(table, "退款原因");

            // 添加数据行
            for (RefundApplication application : refundApplications) {
                addTableCell(table, application.getApplicationNo());
                addTableCell(table, application.getApplicantName());
                addTableCell(table, maskPhone(application.getApplicantPhone()));
                addTableCell(table, application.getOriginalOrderNo());
                addTableCell(table, "¥" + application.getRefundAmount());
                addTableCell(table, application.getRefundTypeDescription());
                addTableCell(table, application.getStatusDescription());
                addTableCell(table, application.getApplicationTime().format(
                    DateTimeFormatter.ofPattern("MM-dd HH:mm")));
                addTableCell(table, truncateText(application.getRefundReason(), 20));
            }

            document.add(table);
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("生成退款申请列表PDF失败", e);
            throw new RuntimeException("生成PDF失败", e);
        }
    }

    @Override
    public byte[] generateRefundStatisticsPdf(List<Object[]> statusCounts) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            // 设置中文字体
            PdfFont font = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            document.setFont(font);

            // 添加标题
            Paragraph title = new Paragraph("退款申请统计报告")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // 生成时间
            Paragraph generateTime = new Paragraph("生成时间：" + 
                java.time.LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(20);
            document.add(generateTime);

            // 创建统计表格
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}))
                    .setWidth(UnitValue.createPercentValue(60))
                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

            // 表头
            addTableHeader(statsTable, "状态");
            addTableHeader(statsTable, "数量");
            addTableHeader(statsTable, "比例");

            // 计算总数
            long totalCount = statusCounts.stream().mapToLong(arr -> (Long) arr[1]).sum();

            // 添加数据行
            for (Object[] statusCount : statusCounts) {
                Integer status = (Integer) statusCount[0];
                Long count = (Long) statusCount[1];
                String statusDesc = getStatusDescription(status);
                String percentage = String.format("%.1f%%", (count * 100.0) / totalCount);

                addTableCell(statsTable, statusDesc);
                addTableCell(statsTable, count.toString());
                addTableCell(statsTable, percentage);
            }

            // 添加总计行
            addTableCell(statsTable, "总计").setBold();
            addTableCell(statsTable, String.valueOf(totalCount)).setBold();
            addTableCell(statsTable, "100.0%").setBold();

            document.add(statsTable);

            // 添加详细说明
            Paragraph explanation = new Paragraph("\n\n状态说明：")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(30);
            document.add(explanation);

            List explanationList = new List()
                    .setFontSize(10)
                    .setMarginLeft(20);
            explanationList.add(new ListItem("待审核：申请已提交，等待审核"));
            explanationList.add(new ListItem("审核通过：申请已通过审核，准备退款"));
            explanationList.add(new ListItem("审核拒绝：申请被拒绝"));
            explanationList.add(new ListItem("退款中：正在处理退款"));
            explanationList.add(new ListItem("退款完成：退款已完成"));
            explanationList.add(new ListItem("退款失败：退款处理失败"));

            document.add(explanationList);
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("生成统计报告PDF失败", e);
            throw new RuntimeException("生成PDF失败", e);
        }
    }

    @Override
    public byte[] generateCustomPdf(String title, String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            // 设置中文字体
            PdfFont font = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            document.setFont(font);

            // 添加标题
            Paragraph titleParagraph = new Paragraph(title)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(titleParagraph);

            // 添加内容
            Paragraph contentParagraph = new Paragraph(content)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(contentParagraph);

            addFooter(document);
            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("生成自定义PDF失败", e);
            throw new RuntimeException("生成PDF失败", e);
        }
    }

    /**
     * 添加表格行
     */
    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "")));
    }

    /**
     * 添加表格头
     */
    private void addTableHeader(Table table, String text) {
        Cell headerCell = new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        table.addHeaderCell(headerCell);
    }

    /**
     * 添加表格单元格
     */
    private Cell addTableCell(Table table, String text) {
        Cell cell = new Cell().add(new Paragraph(text != null ? text : ""))
                .setTextAlignment(TextAlignment.CENTER);
        table.addCell(cell);
        return cell;
    }

    /**
     * 添加页脚
     */
    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("此文档由系统自动生成 - " + 
            java.time.LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30);
        document.add(footer);
    }

    /**
     * 掩码银行卡号
     */
    private String maskBankCardNo(String bankCardNo) {
        if (bankCardNo == null || bankCardNo.length() < 8) {
            return bankCardNo;
        }
        return bankCardNo.substring(0, 4) + "****" + bankCardNo.substring(bankCardNo.length() - 4);
    }

    /**
     * 掩码手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
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
}