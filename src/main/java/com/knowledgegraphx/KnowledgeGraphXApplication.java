package com.knowledgegraphx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KnowledgeGraphXApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeGraphXApplication.class, args);
    }
}
