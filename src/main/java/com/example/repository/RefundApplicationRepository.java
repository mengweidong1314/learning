package com.example.repository;

import com.example.entity.RefundApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 退款申请数据访问层
 * 
 * @author AI Assistant
 */
@Repository
public interface RefundApplicationRepository extends JpaRepository<RefundApplication, Long> {

    /**
     * 根据申请单号查询
     */
    Optional<RefundApplication> findByApplicationNo(String applicationNo);

    /**
     * 根据原订单号查询
     */
    List<RefundApplication> findByOriginalOrderNo(String originalOrderNo);

    /**
     * 根据申请人姓名查询
     */
    List<RefundApplication> findByApplicantName(String applicantName);

    /**
     * 根据状态查询
     */
    List<RefundApplication> findByStatus(Integer status);

    /**
     * 根据申请时间范围查询
     */
    @Query("SELECT r FROM RefundApplication r WHERE r.applicationTime BETWEEN :startTime AND :endTime")
    List<RefundApplication> findByApplicationTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 根据申请人手机号查询
     */
    List<RefundApplication> findByApplicantPhone(String applicantPhone);

    /**
     * 根据申请人邮箱查询
     */
    List<RefundApplication> findByApplicantEmail(String applicantEmail);

    /**
     * 查询指定状态和时间范围内的申请
     */
    @Query("SELECT r FROM RefundApplication r WHERE r.status = :status AND r.applicationTime BETWEEN :startTime AND :endTime")
    List<RefundApplication> findByStatusAndApplicationTimeBetween(@Param("status") Integer status,
                                                                 @Param("startTime") LocalDateTime startTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各状态的申请数量
     */
    @Query("SELECT r.status, COUNT(r) FROM RefundApplication r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * 检查申请单号是否存在
     */
    boolean existsByApplicationNo(String applicationNo);
}