package com.example.service;

import com.example.entity.RefundApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 退款申请服务接口
 * 
 * @author AI Assistant
 */
public interface RefundApplicationService {

    /**
     * 创建退款申请
     */
    RefundApplication createRefundApplication(RefundApplication refundApplication);

    /**
     * 根据ID查询退款申请
     */
    Optional<RefundApplication> findById(Long id);

    /**
     * 根据申请单号查询退款申请
     */
    Optional<RefundApplication> findByApplicationNo(String applicationNo);

    /**
     * 查询所有退款申请
     */
    List<RefundApplication> findAll();

    /**
     * 根据状态查询退款申请
     */
    List<RefundApplication> findByStatus(Integer status);

    /**
     * 根据申请人姓名查询退款申请
     */
    List<RefundApplication> findByApplicantName(String applicantName);

    /**
     * 根据时间范围查询退款申请
     */
    List<RefundApplication> findByApplicationTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 更新退款申请
     */
    RefundApplication updateRefundApplication(RefundApplication refundApplication);

    /**
     * 审核退款申请
     */
    RefundApplication reviewRefundApplication(Long id, Integer status, String reviewer, String remark);

    /**
     * 完成退款
     */
    RefundApplication completeRefund(Long id);

    /**
     * 删除退款申请
     */
    void deleteById(Long id);

    /**
     * 生成申请单号
     */
    String generateApplicationNo();

    /**
     * 统计各状态的申请数量
     */
    List<Object[]> getStatusStatistics();

    /**
     * 初始化测试数据
     */
    void initTestData();
}