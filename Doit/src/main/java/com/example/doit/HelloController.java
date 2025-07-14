package com.example.doit;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

//@Controller: Controller 기능 수행
@Controller
public class HelloController {
//    @GetMapping("/hello") http GET 방식으로 동작
//    @ResponseBody: 메서드의 출력을 그대로 리턴할 것을 명시
    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return "Hello World!";
    }
}
