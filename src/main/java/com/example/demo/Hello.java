package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // 컨트롤러 등록(원격 호출가능한 프로그램으로 등록)
public class Hello {
    // Reflection API: 클래스 정보를 얻고 다룰 수 있는 기능 제공(java.lang.reflect 패키지)
    @RequestMapping("/hello") // URL과 메서드 연결, 접근제어자 상관없이 컨트롤러에 의해 호출 가능
    public String main(){
        return "hello";
    }
}
