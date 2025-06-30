package com.example.demo;

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
    public Coffee(String id, String name) {
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
@RestController
// 중복되는 /coffees 를 @RequestMapping 에 작성
@RequestMapping("/coffees")
class RestApiDemoController {
    //    private final List<Coffee> coffees = new ArrayList<>();
    private final CoffeeRepository coffeeRepository;

    // 생성자
    public RestApiDemoController(CoffeeRepository coffeeRepository) {
        this.coffeeRepository = coffeeRepository;
//        this.coffeeRepository.saveAll(List.of(
//                new Coffee("Cafe1"),
//                new Coffee("Cafe2"),
//                new Coffee("Cafe3"),
//                new Coffee("Cafe4")
//        ));
//        coffees.addAll(List.of(new Coffee("Cafe1"), new Coffee("Cafe2"), new Coffee("Cafe3"), new Coffee("Cafe4")));
    }

    // 반복 가능한(ex. 배열) coffee 객체 반환
    @GetMapping
    Iterable<Coffee> getCoffees() {
        return coffeeRepository.findAll();
    }

    //    @RequestMapping(value = "/coffees", method = RequestMethod.GET) // 요청을 받는데, url과 method를 지정
    // GET 요청만 호용해서 가독성이 좋아진다.
    @GetMapping("/{id}")
    Optional<Coffee> getCoffeeById(@PathVariable String id) {
        return coffeeRepository.findById(id);
//        // 커피 아이디로 커피 검색 후 객체를 반환
//        for (Coffee coffee : coffees) {
//            if (coffee.getId().equals(id)) {
//                return Optional.of(coffee);
//            }
//        }
//        return Optional.empty();    // 없으면 빈 객체 반환(Optional 이라 빈 객체가 허용된다)
    }

    @PostMapping()
    Coffee postCoffee(@RequestBody Coffee coffee) {
        return coffeeRepository.save(coffee);
//        coffees.add(coffee); // coffee 추가
//        return coffee;
    }

    @PutMapping("/{id}")
//    Coffee putCoffee(@PathVariable String id, @RequestBody Coffee coffee) {
        // ResponseEntity<Coffee>를 반환하는 이유? 수정과 생성을 상태 코드로 명확히 구분하기 위해서
    ResponseEntity<Coffee> putCoffee(@PathVariable String id, @RequestBody Coffee coffee) {
        return (!coffeeRepository.existsById(id))
                ? new ResponseEntity<>(coffeeRepository.save(coffee),
                HttpStatus.CREATED)
                : new ResponseEntity<>(coffeeRepository.save(coffee), HttpStatus.OK);
//        int coffeeIdx = -1;
//
//        for (Coffee c : coffees) {
//            if (c.getId().equals(id)) {
//                coffeeIdx = coffees.indexOf(c);     // 찾았으면 인덱스 반환
//                coffees.set(coffeeIdx, coffee);     // 커피 정보 업데이트
//            }
//        }
//        // 만약 수정할 정보가 없으면 새로 추가
//        return (coffeeIdx == -1) ?
//                new ResponseEntity<>(postCoffee(coffee), HttpStatus.CREATED) :
//                new ResponseEntity<>(coffee, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    void deleteCoffee(@PathVariable String id) {
        coffeeRepository.deleteById(id);
        // 커피를 검색해서 해당 아이디가 있으면 삭제 후 true 반환, 없으면 false를 반환
//        coffees.removeIf(coffee -> coffee.getId().equals(id));
    }
}
