package com.example.demo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@Getter // 게터를 롬복에서 사용
@Setter // 세터를 롬복에서 사용
class Coffee {
    private final String id;    // 상수 id
    private String name;        // 이름

    // 생성자
    public Coffee(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // UUID 설정 함수
    public Coffee(String name) {
        this(UUID.randomUUID().toString(), name);
    }
}
