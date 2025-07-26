package com.wbtech.wbscf.price.listener.area;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.wbtech.wbscf.price.domain.dto.area.AreaFreightTemplateDetailDTO;
import com.wbtech.wbscf.price.domain.vo.area.ExcelImportVO;
import com.wbtech.wbscf.price.exception.AreaVersionAssert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域导入监听器 - Optimized Version
 */
@Slf4j
public class AreaFreightImportExcelListener<T extends ExcelImportVO> extends AnalysisEventListener<T> {

    // Constants for better performance
    private static final int INITIAL_CAPACITY = 1000;
    private static final String ROW_KEY_SEPARATOR = "_";
    
    // Excel行数
    private int num;
    // 校验规则信息
    private String analysis;

    // Thread-safe collections for better performance in multi-threaded scenarios
    private final Map<Integer, String> errorMap = new ConcurrentHashMap<>();
    private final Set<String> seenRows = ConcurrentHashMap.newKeySet();
    private final List<T> excelList = Collections.synchronizedList(new ArrayList<>(INITIAL_CAPACITY));

    private final String[] headList;

    // Pre-computed hash codes for faster string comparisons
    private final Map<String, Integer> headerHashCache = new HashMap<>();

    public AreaFreightImportExcelListener(String[] headList) {
        super();
        this.num = 0;
        this.headList = headList != null ? headList.clone() : new String[0];
        
        // Pre-compute header hashes for faster comparison
        for (int i = 0; i < this.headList.length; i++) {
            headerHashCache.put(this.headList[i], this.headList[i].hashCode());
        }
    }

    public String getAnalysis() {
        return analysis;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        if (log.isDebugEnabled()) {
            log.debug("解析到一条头数据:{}", JSON.toJSONString(headMap));
        }
        
        if (!ObjectUtils.isEmpty(headList) && context.readRowHolder().getRowIndex() == 0) {
            validateHeaders(headMap);
        }
    }

    /**
     * Optimized header validation
     */
    private void validateHeaders(Map<Integer, String> headMap) {
        for (int i = 0; i < headList.length; i++) {
            String expectedHeader = headList[i];
            String actualHeader = headMap.get(i);
            
            // Fast comparison using pre-computed hashes and reference equality
            if (actualHeader == null || 
                !expectedHeader.equals(actualHeader) || 
                headerHashCache.get(expectedHeader) != actualHeader.hashCode()) {
                AreaVersionAssert.TEMPLATE_NOT_MATCH.assertEquals(actualHeader, expectedHeader);
            }
        }
    }

    /**
     * 每一条数据解析都会调用 - Optimized version
     */
    @Override
    public void invoke(T dto, AnalysisContext context) {
        // 行号从 0 开始，显示 +1
        int rowNum = context.readRowHolder().getRowIndex() + 1;

        // Fast validation with early returns
        String validateMsg = validateRowFast(dto);
        if (!validateMsg.isEmpty()) {
            errorMap.put(rowNum, validateMsg);
            return;
        }

        // Optimized duplicate check with pre-computed row key
        String rowKey = generateRowKeyOptimized(dto);
        if (!seenRows.add(rowKey)) { // add() returns false if element already exists
            errorMap.put(rowNum, "重复的行数据");
            return;
        }

        excelList.add(dto);
    }

    /**
     * Optimized row validation with early returns and minimal string operations
     */
    private String validateRowFast(T dto) {
        if (dto instanceof AreaFreightTemplateDetailDTO detailDTO) {
            // Use early returns for better performance
            if (isStringEmpty(detailDTO.getCategoryName())) {
                return "品名不能为空";
            }
            if (isStringEmpty(detailDTO.getAreaName())) {
                return "区域名称不能为空";
            }
            if (isStringEmpty(detailDTO.getProvinceName())) {
                return "省不能为空";
            }
            if (isStringEmpty(detailDTO.getCityName())) {
                return "市不能为空";
            }
            if (isStringEmpty(detailDTO.getCountyName())) {
                return "区不能为空";
            }
        }
        return "";
    }

    /**
     * Faster string emptiness check
     */
    private boolean isStringEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Optimized row key generation using StringBuilder for better performance
     */
    private String generateRowKeyOptimized(T dto) {
        if (dto instanceof AreaFreightTemplateDetailDTO detailDTO) {
            // Use StringBuilder for efficient string concatenation
            StringBuilder keyBuilder = new StringBuilder(256); // Pre-allocate capacity
            
            keyBuilder.append(nullSafeString(detailDTO.getCategoryName()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getAreaName()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getProvinceName()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getCityName()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getCountyName()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getTruckTaxExclusiveFreight()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getTrainOpenFreight()))
                     .append(ROW_KEY_SEPARATOR)
                     .append(nullSafeString(detailDTO.getTrainContainerFreight()));
            
            return keyBuilder.toString();
        }
        
        // Fallback for other types - use object's toString with hashCode for uniqueness
        return dto.getClass().getSimpleName() + "_" + dto.hashCode() + "_" + dto.toString();
    }

    /**
     * Null-safe string conversion with minimal object creation
     */
    private String nullSafeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /**
     * 所有数据解析完成都会调用
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Log statistics for monitoring
        if (log.isInfoEnabled()) {
            log.info("Excel解析完成，共处理{}行数据，其中有效数据{}行，错误数据{}行", 
                    context.readRowHolder().getRowIndex() + 1, 
                    excelList.size(), 
                    errorMap.size());
        }
        
        // Optional: Clear temporary data structures to free memory
        seenRows.clear();
        headerHashCache.clear();
    }

    /**
     * 当出现模板数据异常时，结束往下解析，抛出异常
     */
    @Override
    public boolean hasNext(AnalysisContext context) {
        return true;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("Excel解析异常，行号：{}", context.readRowHolder().getRowIndex(), exception);
        super.onException(exception, context);
    }

    /**
     * Get error map - returns defensive copy for thread safety
     */
    public Map<Integer, String> getErrorMap() {
        return new HashMap<>(errorMap);
    }

    /**
     * Get excel list - returns defensive copy for thread safety
     */
    public List<T> getExcelList() {
        synchronized (excelList) {
            return new ArrayList<>(excelList);
        }
    }

    /**
     * Clear all data - useful for memory cleanup
     */
    public void clear() {
        errorMap.clear();
        seenRows.clear();
        synchronized (excelList) {
            excelList.clear();
        }
        headerHashCache.clear();
    }

    /**
     * Get processing statistics
     */
    public ProcessingStats getStats() {
        synchronized (excelList) {
            return new ProcessingStats(excelList.size(), errorMap.size(), seenRows.size());
        }
    }

    /**
     * Statistics holder for monitoring
     */
    public static class ProcessingStats {
        private final int validRows;
        private final int errorRows;
        private final int uniqueRows;

        public ProcessingStats(int validRows, int errorRows, int uniqueRows) {
            this.validRows = validRows;
            this.errorRows = errorRows;
            this.uniqueRows = uniqueRows;
        }

        public int getValidRows() { return validRows; }
        public int getErrorRows() { return errorRows; }
        public int getUniqueRows() { return uniqueRows; }
        public int getTotalProcessed() { return validRows + errorRows; }

        @Override
        public String toString() {
            return String.format("ProcessingStats{valid=%d, errors=%d, unique=%d, total=%d}", 
                               validRows, errorRows, uniqueRows, getTotalProcessed());
        }
    }
}