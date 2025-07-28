package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请实体类
 * 
 * @author AI Assistant
 */
@Entity
@Table(name = "refund_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 申请单号
     */
    @Column(name = "application_no", nullable = false, unique = true, length = 32)
    private String applicationNo;

    /**
     * 申请人姓名
     */
    @Column(name = "applicant_name", nullable = false, length = 50)
    private String applicantName;

    /**
     * 申请人手机号
     */
    @Column(name = "applicant_phone", length = 20)
    private String applicantPhone;

    /**
     * 申请人邮箱
     */
    @Column(name = "applicant_email", length = 100)
    private String applicantEmail;

    /**
     * 原订单号
     */
    @Column(name = "original_order_no", nullable = false, length = 32)
    private String originalOrderNo;

    /**
     * 退款金额
     */
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    @Column(name = "refund_reason", nullable = false, length = 500)
    private String refundReason;

    /**
     * 退款类型：1-全额退款，2-部分退款
     */
    @Column(name = "refund_type", nullable = false)
    private Integer refundType;

    /**
     * 申请状态：0-待审核，1-审核通过，2-审核拒绝，3-退款中，4-退款完成，5-退款失败
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 申请时间
     */
    @Column(name = "application_time", nullable = false)
    private LocalDateTime applicationTime;

    /**
     * 审核时间
     */
    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    /**
     * 审核人
     */
    @Column(name = "reviewer", length = 50)
    private String reviewer;

    /**
     * 审核备注
     */
    @Column(name = "review_remark", length = 500)
    private String reviewRemark;

    /**
     * 退款完成时间
     */
    @Column(name = "refund_time")
    private LocalDateTime refundTime;

    /**
     * 银行卡号
     */
    @Column(name = "bank_card_no", length = 32)
    private String bankCardNo;

    /**
     * 开户银行
     */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /**
     * 开户人姓名
     */
    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
        this.applicationTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = 0; // 默认待审核状态
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (this.status) {
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
     * 获取退款类型描述
     */
    public String getRefundTypeDescription() {
        return switch (this.refundType) {
            case 1 -> "全额退款";
            case 2 -> "部分退款";
            default -> "未知类型";
        };
    }
}