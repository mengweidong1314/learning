package com.wbtech.wbscf.price.service.executor.area;

import com.alibaba.excel.EasyExcel;
import com.wbtech.wbscf.common.enums.StatusEnum;
import com.wbtech.wbscf.price.domain.command.area.AreaFreightImportCommand;
import com.wbtech.wbscf.price.domain.converter.area.AreaFreightVOConverter;
import com.wbtech.wbscf.price.domain.dto.area.AreaFreightTemplateDetailDTO;
import com.wbtech.wbscf.price.domain.entity.AreaFreightTax;
import com.wbtech.wbscf.price.domain.entity.AreaFreightView;
import com.wbtech.wbscf.price.domain.entity.area.AreaDetail;
import com.wbtech.wbscf.price.domain.query.area.areaDetail.AreaDetailQuery;
import com.wbtech.wbscf.price.exception.AreaFreightAssert;
import com.wbtech.wbscf.price.listener.area.AreaFreightImportExcelListener;
import com.wbtech.wbscf.price.repository.area.AreaFreightTaxRepository;
import com.wbtech.wbscf.price.repository.area.AreaFreightViewRepository;
import com.wbtech.wbscf.price.service.executor.area.areaDetail.AreaDetailVerificationExecutor;
import com.wbtech.wbscf.shop.api.CategoryApi;
import com.wbtech.wbscf.shop.api.ShopApi;
import com.wbtech.wbscf.shop.domain.dto.CategoryDTO;
import com.wbtech.wbscf.shop.domain.dto.ShopDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AreaFreightImportCommandExecutor - Optimized Version
 *
 * @author mengweidong
 */
@Slf4j
@Component
public class AreaFreightImportCommandExecutor {

    private static final int BATCH_SIZE = 1000;
    private static final int VALIDATION_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    
    // Pre-compiled validation patterns for better performance
    private static final String LEVEL_SEPARATOR = "-";
    
    @Resource
    private AreaFreightViewRepository areaFreightViewRepository;

    @Resource
    private AreaFreightVOConverter areaFreightVOConverter;

    @Resource
    private ShopApi shopApi;

    @Resource
    private CategoryApi categoryApi;

    @Resource
    private AreaDetailVerificationExecutor areaDetailVerificationExecutor;

    @Resource
    AreaFreightTaxRepository areaFreightTaxRepository;

    // Thread pool for parallel processing
    private final ExecutorService validationExecutor = Executors.newFixedThreadPool(VALIDATION_THREAD_POOL_SIZE);

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<Integer, Object>> execute(AreaFreightImportCommand command) {
        Long companyId = command.getCompanyId();
        
        // Parallel execution of independent operations
        CompletableFuture<ShopDTO> shopFuture = CompletableFuture.supplyAsync(() -> {
            ShopDTO shopDTO = shopApi.getShopByCompanyId(companyId);
            if (ObjectUtils.isEmpty(shopDTO)) {
                throw AreaFreightAssert.COMPANY_SHOP_NOT_EXIST.newException();
            }
            return shopDTO;
        });

        CompletableFuture<List<AreaFreightTemplateDetailDTO>> excelParseFuture = 
            CompletableFuture.supplyAsync(() -> parseExcelFile(command.getFile()));

        CompletableFuture<List<CategoryDTO>> categoriesFuture = 
            CompletableFuture.supplyAsync(() -> getLegalCategoryNames(companyId));

        CompletableFuture<BigDecimal> taxFuture = 
            CompletableFuture.supplyAsync(() -> getFreightTax(companyId));

        // Wait for Excel parsing to complete before area details processing
        List<AreaFreightTemplateDetailDTO> templateDetailDTOList = excelParseFuture.join();
        
        CompletableFuture<List<AreaDetail>> areaDetailsFuture = 
            CompletableFuture.supplyAsync(() -> getLegalAreaDetails(templateDetailDTOList, companyId));

        // Wait for all futures to complete
        try {
            ShopDTO shopDTO = shopFuture.get();
            List<CategoryDTO> existCategoryNames = categoriesFuture.get();
            List<AreaDetail> legalAreaDetails = areaDetailsFuture.get();
            BigDecimal freightTax = taxFuture.get();

            // Pre-build optimized lookup structures
            ValidationContext validationContext = buildValidationContext(existCategoryNames, legalAreaDetails);

            // Parallel validation with batching
            List<AreaFreightTemplateDetailDTO> validList = validateDataInParallel(templateDetailDTOList, validationContext);

            // Batch conversion and save
            if (!validList.isEmpty()) {
                List<AreaFreightView> freightViews = convertToViewsBatch(
                    existCategoryNames, legalAreaDetails, validList, companyId, command, freightTax, shopDTO);
                
                // Clear existing data and save new data
                areaFreightViewRepository.deleteAllByCompanyIdAndAreaFreightVersionIsNull(companyId);
                saveNewDataInBatches(freightViews);
            }

            return ResponseEntity.ok(new HashMap<>());
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during parallel execution", e);
        }
    }

    /**
     * Optimized validation context with pre-built lookup structures
     */
    private static class ValidationContext {
        final Map<String, CategoryDTO> categoryMap;
        final Set<String> areaKeySet;
        final Set<String> provinceKeySet;
        final Set<String> cityKeySet;
        final Set<String> countyKeySet;
        final Map<String, String> areaNameToProvinceCode;
        final Map<String, String> areaNameToCityCode;
        final Map<String, String> areaNameToCountyCode;
        final Map<String, Long> areaNameToAreaId;

        ValidationContext(Map<String, CategoryDTO> categoryMap, Set<String> areaKeySet, 
                         Set<String> provinceKeySet, Set<String> cityKeySet, Set<String> countyKeySet,
                         Map<String, String> areaNameToProvinceCode, Map<String, String> areaNameToCityCode,
                         Map<String, String> areaNameToCountyCode, Map<String, Long> areaNameToAreaId) {
            this.categoryMap = categoryMap;
            this.areaKeySet = areaKeySet;
            this.provinceKeySet = provinceKeySet;
            this.cityKeySet = cityKeySet;
            this.countyKeySet = countyKeySet;
            this.areaNameToProvinceCode = areaNameToProvinceCode;
            this.areaNameToCityCode = areaNameToCityCode;
            this.areaNameToCountyCode = areaNameToCountyCode;
            this.areaNameToAreaId = areaNameToAreaId;
        }
    }

    private ValidationContext buildValidationContext(List<CategoryDTO> existCategoryNames, List<AreaDetail> legalAreaDetails) {
        // Build category map with better key structure
        Map<String, CategoryDTO> categoryMap = existCategoryNames.parallelStream()
            .filter(c -> StringUtils.isNotBlank(c.getName()))
            .collect(Collectors.toConcurrentMap(
                c -> c.getName().trim(), 
                Function.identity(), 
                (existing, replacement) -> existing
            ));

        // Build area lookup sets in parallel
        Set<String> areaKeySet = legalAreaDetails.parallelStream()
            .map(this::buildFullAreaKey)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toConcurrentHashMap()::putIfAbsent);

        Set<String> provinceKeySet = legalAreaDetails.parallelStream()
            .map(this::buildFullProvinceKey)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        Set<String> cityKeySet = legalAreaDetails.parallelStream()
            .map(this::buildFullCityKey)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        Set<String> countyKeySet = legalAreaDetails.parallelStream()
            .map(this::buildFullCountyKey)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        // Build area mapping in parallel
        Map<String, String> areaNameToProvinceCode = new ConcurrentHashMap<>();
        Map<String, String> areaNameToCityCode = new ConcurrentHashMap<>();
        Map<String, String> areaNameToCountyCode = new ConcurrentHashMap<>();
        Map<String, Long> areaNameToAreaId = new ConcurrentHashMap<>();

        legalAreaDetails.parallelStream().forEach(detail -> {
            String fullAreaKey = buildFullAreaKey(detail);
            String fullProvinceKey = buildFullProvinceKey(detail);
            String fullCityKey = buildFullCityKey(detail);
            String fullCountyKey = buildFullCountyKey(detail);
            
            areaNameToProvinceCode.put(fullProvinceKey, detail.getProvinceCode());
            areaNameToCityCode.put(fullCityKey, detail.getCityCode());
            areaNameToCountyCode.put(fullCountyKey, detail.getCountyCode());
            areaNameToAreaId.put(fullAreaKey, detail.getAreaId());
        });

        return new ValidationContext(categoryMap, areaKeySet, provinceKeySet, cityKeySet, countyKeySet,
                                   areaNameToProvinceCode, areaNameToCityCode, areaNameToCountyCode, areaNameToAreaId);
    }

    private List<AreaFreightTemplateDetailDTO> validateDataInParallel(
            List<AreaFreightTemplateDetailDTO> templateDetailDTOList, 
            ValidationContext context) {
        
        // Split data into chunks for parallel processing
        int chunkSize = Math.max(1, templateDetailDTOList.size() / VALIDATION_THREAD_POOL_SIZE);
        List<List<AreaFreightTemplateDetailDTO>> chunks = partitionList(templateDetailDTOList, chunkSize);
        
        List<CompletableFuture<List<AreaFreightTemplateDetailDTO>>> futures = chunks.stream()
            .map(chunk -> CompletableFuture.supplyAsync(() -> validateChunk(chunk, context), validationExecutor))
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();
    }

    private List<AreaFreightTemplateDetailDTO> validateChunk(
            List<AreaFreightTemplateDetailDTO> chunk, 
            ValidationContext context) {
        
        List<AreaFreightTemplateDetailDTO> validList = new ArrayList<>();
        
        for (int i = 0; i < chunk.size(); i++) {
            AreaFreightTemplateDetailDTO dto = chunk.get(i);
            String errorMessage = validateRowOptimized(dto, context);
            
            if (errorMessage.isEmpty()) {
                validList.add(dto);
            } else {
                // Calculate actual row number (simplified for this optimization)
                int actualRowNum = i + 1;
                errorMessage = "第" + actualRowNum + "行" + errorMessage;
                throw AreaFreightAssert.FILE_IMPORT_FAILED.newException(Map.of("message", errorMessage));
            }
        }
        
        return validList;
    }

    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitions.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return partitions;
    }

    private BigDecimal getFreightTax(Long companyId) {
        AreaFreightTax areaFreightTax = areaFreightTaxRepository.getByCompanyId(companyId);
        return ObjectUtils.isEmpty(areaFreightTax) ? 
            BigDecimal.valueOf(1.134) : areaFreightTax.getFreightTax();
    }

    private int getCategoryLevel(String categoryName) {
        if (StringUtils.isBlank(categoryName)) return 0;
        return (int) categoryName.chars().filter(ch -> ch == '-').count() + 1;
    }

    /**
     * Optimized row validation with pre-built lookup structures
     */
    private String validateRowOptimized(AreaFreightTemplateDetailDTO dto, ValidationContext context) {
        String categoryName = dto.getCategoryName();
        String areaName = dto.getAreaName();
        String provinceName = dto.getProvinceName();
        String cityName = dto.getCityName();
        String countyName = dto.getCountyName();

        // Early validation for required fields
        if (StringUtils.isEmpty(categoryName)) {
            return "【品名为空】";
        }

        // Category validation with optimized level checking
        int level = getCategoryLevel(categoryName);
        if (level < 1 || level > 3) {
            return "【品名格式错误，必须为一级、二级或三级品名】";
        }

        String validationError = validateCategoryByLevel(categoryName, level, context.categoryMap);
        if (!validationError.isEmpty()) {
            return validationError;
        }

        // Area validations using pre-built sets
        if (StringUtils.isBlank(areaName) || !context.areaKeySet.contains(StringUtils.trimToEmpty(areaName))) {
            return StringUtils.isBlank(areaName) ? "【区域名称为空】" : 
                "【区域名称不存在或禁用：" + StringUtils.defaultString(areaName) + "】";
        }

        if (StringUtils.isBlank(provinceName) || !context.provinceKeySet.contains(StringUtils.trimToEmpty(provinceName))) {
            return StringUtils.isBlank(provinceName) ? "【省名称为空】" : 
                "【省名称不存在或禁用：" + StringUtils.defaultString(provinceName) + "】";
        }

        if (StringUtils.isBlank(cityName) || !context.cityKeySet.contains(StringUtils.trimToEmpty(cityName))) {
            return StringUtils.isBlank(cityName) ? "【市名称为空】" : 
                "【市名称不存在或禁用：" + StringUtils.defaultString(cityName) + "】";
        }

        if (StringUtils.isBlank(countyName) || !context.countyKeySet.contains(StringUtils.trimToEmpty(countyName))) {
            return StringUtils.isBlank(countyName) ? "【区名称为空】" : 
                "【区名称不存在或禁用：" + StringUtils.defaultString(countyName) + "】";
        }

        // Freight validation
        if (ObjectUtils.allNull(dto.getTruckTaxExclusiveFreight(), dto.getTrainOpenFreight(), dto.getTrainContainerFreight())) {
            return "【运费未填写，请查看】";
        }

        return "";
    }

    private String validateCategoryByLevel(String categoryName, int level, Map<String, CategoryDTO> categoryMap) {
        switch (level) {
            case 1:
                return categoryMap.containsKey(categoryName.trim()) ? "" : 
                    "【一级品名不存在或禁用：" + categoryName + "】";
            
            case 2:
                String[] parts2 = categoryName.split(LEVEL_SEPARATOR, 2);
                if (parts2.length != 2) {
                    return "【二级品名格式错误，应为：一级品名-二级品名】";
                }
                
                String parentName = parts2[0].trim();
                String currentName = parts2[1].trim();
                
                if (StringUtils.isBlank(parentName)) return "【父品名（一级）为空：" + parentName + "】";
                if (StringUtils.isBlank(currentName)) return "【二级品名为空：" + parentName + "】";
                if (!categoryMap.containsKey(parentName)) return "【父品名（一级）不存在或禁用：" + parentName + "】";
                if (!categoryMap.containsKey(currentName)) return "【二级品名不存在或禁用：" + currentName + "】";
                
                return "";
            
            case 3:
                String[] parts3 = categoryName.split(LEVEL_SEPARATOR, 3);
                if (parts3.length != 3) {
                    return "【三级品名格式错误，应为：一级品名-二级品名-三级品名】";
                }
                
                String grandParent = parts3[0].trim();
                String parent = parts3[1].trim();
                String current = parts3[2].trim();
                
                if (StringUtils.isBlank(grandParent)) return "【父品名（一级）为空：" + grandParent + "】";
                if (StringUtils.isBlank(parent)) return "【父品名（二级）为空：" + parent + "】";
                if (StringUtils.isBlank(current)) return "【三级品名为空：" + current + "】";
                if (!categoryMap.containsKey(grandParent)) return "【父品名（一级）不存在或禁用：" + grandParent + "】";
                if (!categoryMap.containsKey(parent)) return "【父品名（二级）不存在或禁用：" + parent + "】";
                if (!categoryMap.containsKey(current)) return "【三级品名不存在或禁用：" + current + "】";
                
                return "";
            
            default:
                return "【无效的品名层级】";
        }
    }

    /**
     * Parse Excel file (same as original, no significant optimization needed here)
     */
    private List<AreaFreightTemplateDetailDTO> parseExcelFile(MultipartFile file) {
        AreaFreightImportExcelListener<AreaFreightTemplateDetailDTO> listener = 
            new AreaFreightImportExcelListener<>(AreaFreightTemplateDetailDTO.HEAD_NAMES);
        try {
            EasyExcel.read(file.getInputStream(), AreaFreightTemplateDetailDTO.class, listener).sheet().doRead();
        } catch (IOException e) {
            throw AreaFreightAssert.FILE_IMPORT_FAILED.newException();
        }
        Map<Integer, String> errorMap = listener.getErrorMap();
        if (!errorMap.isEmpty()) {
            throw AreaFreightAssert.FILE_IMPORT_FAILED.newException(Map.of("message", errorMap.toString()));
        }
        return listener.getExcelList();
    }

    private List<CategoryDTO> getLegalCategoryNames(Long companyId) {
        return categoryApi.getCategoryByCompanyId(companyId).parallelStream()
            .filter(item -> item.getStatus().equals(StatusEnum.ENABLED))
            .toList();
    }

    private List<AreaDetail> getLegalAreaDetails(List<AreaFreightTemplateDetailDTO> dtoList, Long companyId) {
        // Use parallel stream and collect unique queries first
        Set<AreaDetailQuery> uniqueQueries = dtoList.parallelStream()
            .map(dto -> {
                AreaDetailQuery query = new AreaDetailQuery();
                query.setProvinceName(dto.getProvinceName());
                query.setCityName(dto.getCityName());
                query.setCountyName(dto.getCountyName());
                query.setAreaName(dto.getAreaName());
                return query;
            })
            .collect(Collectors.toSet());

        return areaDetailVerificationExecutor.getLegalAreaDetails(new ArrayList<>(uniqueQueries), companyId);
    }

    /**
     * Optimized batch conversion to views
     */
    private List<AreaFreightView> convertToViewsBatch(List<CategoryDTO> existCategoryNames, 
                                                      List<AreaDetail> legalAreaDetails, 
                                                      List<AreaFreightTemplateDetailDTO> list, 
                                                      Long companyId, 
                                                      AreaFreightImportCommand command, 
                                                      BigDecimal freightTax, 
                                                      ShopDTO shopDTO) {
        
        // Pre-build all mapping structures (moved to ValidationContext)
        ValidationContext context = buildValidationContext(existCategoryNames, legalAreaDetails);
        
        // Build category lookup with level information
        Map<String, CategoryDTO> categoryLevelMap = existCategoryNames.parallelStream()
            .filter(category -> category.getName() != null && !category.getName().trim().isEmpty())
            .collect(Collectors.toConcurrentMap(
                category -> category.getName().trim() + "__LVL" + category.getLevel(),
                Function.identity(),
                (existing, replacement) -> {
                    log.warn("Duplicate category found: {}", existing.getName());
                    return existing;
                }
            ));

        return list.parallelStream().map(dto -> {
            AreaFreightView view = areaFreightVOConverter.assemble(dto);

            // Optimized category matching
            String categoryName = dto.getCategoryName().trim();
            String[] parts = categoryName.split(LEVEL_SEPARATOR);
            int inputLevel = Math.min(parts.length, 3);
            String targetName = parts[inputLevel - 1].trim();
            String lookupKey = targetName + "__LVL" + inputLevel;
            
            CategoryDTO targetCategory = categoryLevelMap.get(lookupKey);
            if (targetCategory != null) {
                view.setCategoryId(targetCategory.getId());
                view.setCategoryName(targetCategory.getName());
            } else {
                String levelName = inputLevel == 1 ? "一级" : inputLevel == 2 ? "二级" : "三级";
                throw AreaFreightAssert.FILE_IMPORT_FAILED.newException(
                    Map.of("message", "找不到对应的" + levelName + "类目信息" + categoryName));
            }

            // Set area information using pre-built maps
            view.setAreaId(context.areaNameToAreaId.get(dto.getAreaName()));
            view.setProvinceCode(context.areaNameToProvinceCode.get(dto.getProvinceName()));
            view.setCityCode(context.areaNameToCityCode.get(dto.getCityName()));
            view.setCountyCode(context.areaNameToCountyCode.get(dto.getCountyName()));

            // Set freight and other fields
            if (Objects.nonNull(dto.getTruckTaxExclusiveFreight())) {
                view.setTruckFreight(dto.getTruckTaxExclusiveFreight().multiply(freightTax));
            }
            
            view.setCompanyId(companyId);
            view.setShopId(shopDTO.getId());
            view.setShopName(shopDTO.getName());
            view.setCreatedUserId(command.getCreatedUserId());
            view.setCreatedName(command.getCreatedName());
            view.setModifiedUserId(command.getCreatedUserId());
            view.setModifiedName(command.getCreatedName());

            return view;
        }).toList();
    }

    private String buildFullAreaKey(AreaDetail detail) {
        return Optional.ofNullable(detail.getAreaName()).orElse("");
    }

    private String buildFullProvinceKey(AreaDetail detail) {
        return Optional.ofNullable(detail.getProvinceName()).orElse("");
    }

    private String buildFullCityKey(AreaDetail detail) {
        return Optional.ofNullable(detail.getCityName()).orElse("");
    }

    private String buildFullCountyKey(AreaDetail detail) {
        return Optional.ofNullable(detail.getCountyName()).orElse("");
    }

    /**
     * Optimized batch saving with configurable batch size
     */
    private void saveNewDataInBatches(List<AreaFreightView> allFreightList) {
        if (allFreightList.isEmpty()) return;
        
        for (int startIndex = 0; startIndex < allFreightList.size(); startIndex += BATCH_SIZE) {
            int endIndex = Math.min(startIndex + BATCH_SIZE, allFreightList.size());
            List<AreaFreightView> batch = allFreightList.subList(startIndex, endIndex);
            areaFreightViewRepository.saveAllAndFlush(batch);
        }
    }
}