package com.example.service.impl;

import com.example.entity.RefundApplication;
import com.example.repository.RefundApplicationRepository;
import com.example.service.RefundApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 退款申请服务实现类
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationServiceImpl implements RefundApplicationService {

    private final RefundApplicationRepository refundApplicationRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public RefundApplication createRefundApplication(RefundApplication refundApplication) {
        if (refundApplication.getApplicationNo() == null) {
            refundApplication.setApplicationNo(generateApplicationNo());
        }
        return refundApplicationRepository.save(refundApplication);
    }

    @Override
    public Optional<RefundApplication> findById(Long id) {
        return refundApplicationRepository.findById(id);
    }

    @Override
    public Optional<RefundApplication> findByApplicationNo(String applicationNo) {
        return refundApplicationRepository.findByApplicationNo(applicationNo);
    }

    @Override
    public List<RefundApplication> findAll() {
        return refundApplicationRepository.findAll();
    }

    @Override
    public List<RefundApplication> findByStatus(Integer status) {
        return refundApplicationRepository.findByStatus(status);
    }

    @Override
    public List<RefundApplication> findByApplicantName(String applicantName) {
        return refundApplicationRepository.findByApplicantName(applicantName);
    }

    @Override
    public List<RefundApplication> findByApplicationTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return refundApplicationRepository.findByApplicationTimeBetween(startTime, endTime);
    }

    @Override
    @Transactional
    public RefundApplication updateRefundApplication(RefundApplication refundApplication) {
        return refundApplicationRepository.save(refundApplication);
    }

    @Override
    @Transactional
    public RefundApplication reviewRefundApplication(Long id, Integer status, String reviewer, String remark) {
        Optional<RefundApplication> optionalApplication = refundApplicationRepository.findById(id);
        if (optionalApplication.isPresent()) {
            RefundApplication application = optionalApplication.get();
            application.setStatus(status);
            application.setReviewer(reviewer);
            application.setReviewRemark(remark);
            application.setReviewTime(LocalDateTime.now());
            return refundApplicationRepository.save(application);
        }
        throw new RuntimeException("退款申请不存在，ID: " + id);
    }

    @Override
    @Transactional
    public RefundApplication completeRefund(Long id) {
        Optional<RefundApplication> optionalApplication = refundApplicationRepository.findById(id);
        if (optionalApplication.isPresent()) {
            RefundApplication application = optionalApplication.get();
            application.setStatus(4); // 退款完成
            application.setRefundTime(LocalDateTime.now());
            return refundApplicationRepository.save(application);
        }
        throw new RuntimeException("退款申请不存在，ID: " + id);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        refundApplicationRepository.deleteById(id);
    }

    @Override
    public String generateApplicationNo() {
        String prefix = "RF";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomNum = String.format("%04d", random.nextInt(10000));
        return prefix + timestamp + randomNum;
    }

    @Override
    public List<Object[]> getStatusStatistics() {
        return refundApplicationRepository.countByStatus();
    }

    @Override
    @PostConstruct
    @Transactional
    public void initTestData() {
        // 检查是否已有数据
        if (refundApplicationRepository.count() > 0) {
            log.info("数据库中已存在数据，跳过初始化");
            return;
        }

        log.info("开始初始化测试数据...");

        // 创建测试数据
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
        String[] phones = {"13800138001", "13800138002", "13800138003", "13800138004", 
                          "13800138005", "13800138006", "13800138007", "13800138008"};
        String[] emails = {"zhangsan@example.com", "lisi@example.com", "wangwu@example.com", 
                          "zhaoliu@example.com", "qianqi@example.com", "sunba@example.com",
                          "zhoujiu@example.com", "wushi@example.com"};
        String[] reasons = {"商品质量问题", "不满意商品", "商品与描述不符", "多拍错拍", 
                          "商品缺货", "发货时间过长", "商品损坏", "其他原因"};
        String[] bankNames = {"中国工商银行", "中国建设银行", "中国农业银行", "中国银行",
                            "招商银行", "交通银行", "中信银行", "民生银行"};

        for (int i = 0; i < 20; i++) {
            int index = i % names.length;
            
            RefundApplication application = RefundApplication.builder()
                    .applicationNo(generateApplicationNo())
                    .applicantName(names[index])
                    .applicantPhone(phones[index])
                    .applicantEmail(emails[index])
                    .originalOrderNo("ORD" + System.currentTimeMillis() + String.format("%03d", i))
                    .refundAmount(new BigDecimal(100 + random.nextInt(1000) + "." + random.nextInt(100)))
                    .refundReason(reasons[random.nextInt(reasons.length)])
                    .refundType(random.nextInt(2) + 1) // 1或2
                    .status(random.nextInt(6)) // 0-5
                    .applicationTime(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .bankCardNo("6222" + String.format("%015d", random.nextLong() % 1000000000000000L))
                    .bankName(bankNames[random.nextInt(bankNames.length)])
                    .accountHolder(names[index])
                    .build();

            // 如果状态大于0，添加审核信息
            if (application.getStatus() > 0) {
                application.setReviewTime(application.getApplicationTime().plusDays(1));
                application.setReviewer("管理员" + (random.nextInt(3) + 1));
                if (application.getStatus() == 2) {
                    application.setReviewRemark("申请材料不完整");
                } else {
                    application.setReviewRemark("审核通过");
                }
            }

            // 如果状态是退款完成，添加退款时间
            if (application.getStatus() == 4) {
                application.setRefundTime(application.getReviewTime().plusDays(1));
            }

            refundApplicationRepository.save(application);
        }

        log.info("测试数据初始化完成，共创建 {} 条记录", 20);
    }
}