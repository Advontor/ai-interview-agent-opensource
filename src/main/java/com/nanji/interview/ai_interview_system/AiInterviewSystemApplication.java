package com.nanji.interview.ai_interview_system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nanji.interview.ai_interview_system.modules.**.mapper")
public class AiInterviewSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiInterviewSystemApplication.class, args);
    }
}