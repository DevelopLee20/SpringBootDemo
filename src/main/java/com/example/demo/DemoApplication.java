package com.example.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CRUD Repository 인터페이스를 상속할 인터페이스
interface CoffeeRepository extends CrudRepository<Coffee, String> {
}

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

// 이 클래스를 엔티티로 사용: 데이터베이스 테이블과 매핑된다는 의미의 JPA 어노테이션
@Getter // 게터를 롬복에서 사용
@Setter // 세터를 롬복에서 사용
@Entity
class Coffee {
    // @Id DB 필드의 ID 표시
    @Id
    private String id;          // 상수 id
    private String name;        // 이름

    // 생성자
    // JsonCreator: final로 선언된 id도 setter 사용 가능
    // JsonProperty: JSON으로 객체를 바꾸라고 요청하는 것
    @JsonCreator
    public Coffee(@JsonProperty("id") String id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    // UUID 설정 생성자
    public Coffee(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public Coffee() {
    }
}

// 초기 데이터를 생성하는 컴포넌트
@Component
class DataLoader {
    private final CoffeeRepository coffeeRepository;

    public DataLoader(CoffeeRepository coffeeRepository) {
        this.coffeeRepository = coffeeRepository;
    }

    @PostConstruct
    private void loadData() {
        coffeeRepository.saveAll(List.of(
                new Coffee("Cafe1"),
                new Coffee("Cafe2"),
                new Coffee("Cafe3"),
                new Coffee("Cafe4")
        ));
    }
}

// MVC를 연결하는 어노테이션
// 중복되는 /coffees 를 @RequestMapping 에 작성
@RestController
@RequestMapping("/coffees")
class RestApiDemoController {
    private final CoffeeRepository coffeeRepository;

    // 생성자
    public RestApiDemoController(CoffeeRepository coffeeRepository) {
        this.coffeeRepository = coffeeRepository;
    }

    // 반복 가능한(ex. 배열) coffee 객체 반환
    @GetMapping
    Iterable<Coffee> getCoffees() {
        return coffeeRepository.findAll();
    }

    // GET 요청만 허용해서 가독성이 좋아진다.
    @GetMapping("/{id}")
    Optional<Coffee> getCoffeeById(@PathVariable String id) {
        return coffeeRepository.findById(id);
    }

    @PostMapping()
    Coffee postCoffee(@RequestBody Coffee coffee) {
        return coffeeRepository.save(coffee);
    }

    @PutMapping("/{id}")
    ResponseEntity<Coffee> putCoffee(@PathVariable String id, @RequestBody Coffee coffee) {
        return (coffeeRepository.existsById(id))
                ? new ResponseEntity<>(coffeeRepository.save(coffee),
                HttpStatus.OK)
                : new ResponseEntity<>(coffeeRepository.save(coffee),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    void deleteCoffee(@PathVariable String id) {
        coffeeRepository.deleteById(id);
        // 커피를 검색해서 해당 아이디가 있으면 삭제 후 true 반환, 없으면 false를 반환
    }
}
