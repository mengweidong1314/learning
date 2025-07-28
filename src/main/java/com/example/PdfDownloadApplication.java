package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PDF下载演示应用 - 退款申请系统
 * 
 * @author AI Assistant
 * @version 1.0.0
 */
@SpringBootApplication
public class PdfDownloadApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfDownloadApplication.class, args);
        System.out.println("===================================");
        System.out.println("PDF下载演示系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("API文档: http://localhost:8080/api/refunds");
        System.out.println("===================================");
    }
}