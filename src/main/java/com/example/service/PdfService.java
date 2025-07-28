package com.example.service;

import com.example.entity.RefundApplication;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * PDF生成服务接口
 * 
 * @author AI Assistant
 */
public interface PdfService {

    /**
     * 生成单个退款申请PDF
     * 
     * @param refundApplication 退款申请信息
     * @return PDF字节数组
     */
    byte[] generateRefundApplicationPdf(RefundApplication refundApplication);

    /**
     * 生成退款申请列表PDF
     * 
     * @param refundApplications 退款申请列表
     * @return PDF字节数组
     */
    byte[] generateRefundApplicationListPdf(List<RefundApplication> refundApplications);

    /**
     * 生成退款申请统计报告PDF
     * 
     * @param statusCounts 状态统计数据
     * @return PDF字节数组
     */
    byte[] generateRefundStatisticsPdf(List<Object[]> statusCounts);

    /**
     * 生成自定义内容PDF
     * 
     * @param title 标题
     * @param content 内容
     * @return PDF字节数组
     */
    byte[] generateCustomPdf(String title, String content);
}