#!/bin/bash

echo "=============================================="
echo "PDF下载功能测试脚本"
echo "=============================================="

# 设置基础URL
BASE_URL="http://localhost:8080"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查服务是否启动
check_service() {
    echo -e "${YELLOW}检查服务状态...${NC}"
    if curl -s "$BASE_URL/api/refunds" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 服务正在运行${NC}"
        return 0
    else
        echo -e "${RED}❌ 服务未启动，请先启动应用：mvn spring-boot:run${NC}"
        return 1
    fi
}

# 测试API响应
test_api() {
    echo -e "\n${YELLOW}测试API响应...${NC}"
    response=$(curl -s "$BASE_URL/api/refunds")
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ API响应正常${NC}"
        echo "返回数据: $response"
    else
        echo -e "${RED}❌ API请求失败${NC}"
    fi
}

# 下载单个退款申请PDF
download_single_pdf() {
    echo -e "\n${YELLOW}下载单个退款申请PDF...${NC}"
    
    # 获取第一个退款申请的ID
    refund_id=$(curl -s "$BASE_URL/api/refunds" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$refund_id" ]; then
        echo "获取到退款申请ID: $refund_id"
        curl -o "退款申请_${refund_id}.pdf" "$BASE_URL/api/refunds/${refund_id}/pdf"
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ 单个PDF下载成功: 退款申请_${refund_id}.pdf${NC}"
        else
            echo -e "${RED}❌ 单个PDF下载失败${NC}"
        fi
    else
        echo -e "${RED}❌ 未找到退款申请数据${NC}"
    fi
}

# 下载退款申请列表PDF
download_list_pdf() {
    echo -e "\n${YELLOW}下载退款申请列表PDF...${NC}"
    curl -o "退款申请列表.pdf" "$BASE_URL/api/refunds/list/pdf"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 列表PDF下载成功: 退款申请列表.pdf${NC}"
    else
        echo -e "${RED}❌ 列表PDF下载失败${NC}"
    fi
}

# 下载统计报告PDF
download_statistics_pdf() {
    echo -e "\n${YELLOW}下载统计报告PDF...${NC}"
    curl -o "退款申请统计报告.pdf" "$BASE_URL/api/refunds/statistics/pdf"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 统计报告PDF下载成功: 退款申请统计报告.pdf${NC}"
    else
        echo -e "${RED}❌ 统计报告PDF下载失败${NC}"
    fi
}

# 下载按状态筛选的PDF
download_by_status_pdf() {
    echo -e "\n${YELLOW}下载按状态筛选的PDF...${NC}"
    
    # 下载待审核状态的申请
    curl -o "待审核退款申请.pdf" "$BASE_URL/api/refunds/list/pdf?status=0"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 待审核状态PDF下载成功: 待审核退款申请.pdf${NC}"
    else
        echo -e "${RED}❌ 待审核状态PDF下载失败${NC}"
    fi
    
    # 下载已通过状态的申请
    curl -o "已通过退款申请.pdf" "$BASE_URL/api/refunds/list/pdf?status=1"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 已通过状态PDF下载成功: 已通过退款申请.pdf${NC}"
    else
        echo -e "${RED}❌ 已通过状态PDF下载失败${NC}"
    fi
}

# 下载自定义内容PDF
download_custom_pdf() {
    echo -e "\n${YELLOW}下载自定义内容PDF...${NC}"
    curl -X POST \
         -H "Content-Type: application/json" \
         -d '{"title":"测试报告","content":"这是一个自定义的PDF内容示例\\n包含多行文本\\n用于演示PDF生成功能"}' \
         -o "自定义内容.pdf" \
         "$BASE_URL/api/refunds/custom/pdf"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 自定义内容PDF下载成功: 自定义内容.pdf${NC}"
    else
        echo -e "${RED}❌ 自定义内容PDF下载失败${NC}"
    fi
}

# 显示下载的文件
show_downloaded_files() {
    echo -e "\n${YELLOW}下载的PDF文件:${NC}"
    ls -la *.pdf 2>/dev/null
    if [ $? -eq 0 ]; then
        echo -e "\n${GREEN}所有PDF文件下载完成！${NC}"
        echo -e "${YELLOW}提示: 可以使用PDF阅读器打开这些文件查看内容${NC}"
    else
        echo -e "${RED}未找到下载的PDF文件${NC}"
    fi
}

# 显示使用说明
show_usage() {
    echo -e "\n${YELLOW}============== 使用说明 ==============${NC}"
    echo "1. 确保应用已启动: mvn spring-boot:run"
    echo "2. 访问Web界面: http://localhost:8080"
    echo "3. 访问H2控制台: http://localhost:8080/h2-console"
    echo "4. API文档:"
    echo "   - GET  /api/refunds                    # 获取所有退款申请"
    echo "   - GET  /api/refunds/{id}               # 获取单个退款申请"
    echo "   - GET  /api/refunds/{id}/pdf           # 下载单个退款申请PDF"
    echo "   - GET  /api/refunds/list/pdf           # 下载退款申请列表PDF"
    echo "   - GET  /api/refunds/statistics/pdf     # 下载统计报告PDF"
    echo "   - POST /api/refunds/custom/pdf         # 下载自定义内容PDF"
    echo -e "${YELLOW}=======================================${NC}"
}

# 主函数
main() {
    if check_service; then
        test_api
        download_single_pdf
        download_list_pdf
        download_statistics_pdf
        download_by_status_pdf
        download_custom_pdf
        show_downloaded_files
    fi
    show_usage
}

# 执行主函数
main