package com.example.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CRUD Repository 인터페이스를 상속할 인터페이스
interface CoffeeRepository extends CrudRepository<Coffee, String> {
}

// @Data equals(), hashCode(), toString() 메서드를 생성해 데이터 클래스를 만들어준다.
// @NoArgsConstructor 롬복에 매개변수 없는 생성자를 생성하도록 지시
// @AllArgsConstructor 롬복에 각 멤버 변수의 매개변수를 가진 생성자를 만들도록 지시
// @JsonIgnoreProperties(ignoreUnknown = true) 응답 필드 중 멤버 변수가 없으면, 역직렬화시 이를 무시하도록 지시

// @ConfigurationPropertiesScan: main이 application.properties를 스캔하도록 설정한다.
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args); // 애플리케이션 실행
    }

    //    @Bean 빈 생성
    // 외부에서 빈 생성 후 @ConfigurationProperties 사용
    @Bean
    @ConfigurationProperties(prefix = "droid")
    Droid createDroid() {
        return new Droid();
    }

    //    @Bean : Redis 연동을 위한 RedisOperations Bean(빈) 생성
    @Bean
    public RedisOperations<String, Aircraft> redisOperations(RedisConnectionFactory factory) {
        // 객체를 JSON 형태로 직렬화/역직렬화 하기 위한 설정
        Jackson2JsonRedisSerializer<Aircraft> serializer = new Jackson2JsonRedisSerializer<>(Aircraft.class);

        // RedisTemplate: Redis와 데이터 입/출력을 담당
        RedisTemplate<String, Aircraft> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);                 // Redis 연결 설정
        template.setDefaultSerializer(serializer);              // Jackson에 의한 값 직렬화
        template.setKeySerializer(new StringRedisSerializer()); // 키 직렬화 -> 문자열

        return template;    // RedisOperations: Redis에 값 저장 및 조회 가능
    }
}

@EnableScheduling   // 이 클래스를 스케줄링(주기적인 작업) 등록
@Component          // 스프링이 빈으로 자동 등록하도록 함
class PlaneFinderPoller {
    private final RedisConnectionFactory connectionFactory;                                 // Redis 연결
    private final RedisOperations<String, Aircraft> redisOperations;                        // Redis 조작 인터페이스
    // 비동기 방식으로 외부 API 호출하는 WebFlux
    private final WebClient client = WebClient.create("https://localhost:7634/aircraft");

    // 생성자
    PlaneFinderPoller(RedisConnectionFactory connectionFactory,
                      RedisOperations<String, Aircraft> redisOperations) {
        this.connectionFactory = connectionFactory;
        this.redisOperations = redisOperations;
    }

    @Scheduled(fixedRate = 5000) // 5초(5000ms) 마다 실행, 스케줄링에 의해 실행됨
    private void pollPlanes() {
        // Redis DB 초기화
        connectionFactory.getConnection().serverCommands().flushDb();

        client.get()                                            // GET 요청
                .retrieve()                                     // 응답 받기
                .bodyToFlux(Aircraft.class)                     // JSON 응답을 Aircraft 리스트로 반환(Flux는 stream 형식)
                .filter(plane -> !plane.getReg()
                        .isEmpty()
                ).toStream()                                    // Java Stream으로 변환
                .forEach(ac -> redisOperations.opsForValue().set(ac.getReg(), ac)); // Redis에 등록번호를 key로 저장

        // 저장된 데이터를 모두 꺼내서 출력
        redisOperations.opsForValue()
                .getOperations()
                .keys("*")
                .forEach(ac -> System.out.println(redisOperations.opsForValue().get(ac)));
    }
}

@Data // getter, setter, toString, equals 등을 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 생성
@JsonIgnoreProperties(ignoreUnknown = true) // JSON에 정의되지 않은 필드는 무시
class Aircraft {
    @Id
    private Long id;
    private String callsign, squawk, reg, flightno, route, type, category;
    private int altitude, heading, speed;

    // @JsonProperty: Json 형식으로 객체를 바꾸라고 요청하는 것
    @JsonProperty("vert_rate")  // JSON의 vert_rate -> vertRate로 배핑
    private int vertRate;

    @JsonProperty("selected_altitude")
    private int selectedAltitude;
    private double lat, lon, barometer;

    @JsonProperty("polar_distance")
    private double polarDistance;

    @JsonProperty("is_adsb")
    private boolean isADSB;

    @JsonProperty("is_on_ground")
    private boolean isOnGround;

    // Instant: 정확한 시간 시점 객체(정리: 상수처럼 쓰일 시간 의미)
    @JsonProperty("last_seen_time")
    private Instant lastSeenTime;

    @JsonProperty("pos_update_time")
    private Instant posUpdateTime;

    @JsonProperty("bds40_seen_time")
    private Instant bds40SeenTime;

    public String getLastSeenTime() {
        return lastSeenTime.toString();
    }

    // String을 int로 변환하는 getter/setter
    public void setLastSeenTime(String lastSeenTime) {
        if (null != lastSeenTime) {
            this.lastSeenTime = Instant.parse(lastSeenTime);
        } else {
            // Instant.ofEpochSecond(time); 1970년 1월 1일 0시 0분 0초 기준 time 초 후 시간을 반환
            // 반환된 데이터는 Instant 타입을 가진다.
            this.lastSeenTime = Instant.ofEpochSecond(0);
        }
    }

    public String getPosUpdateTime() {
        return posUpdateTime.toString();
    }

    public void setPosUpdateTime(String posUpdateTime) {
        if (null != posUpdateTime) {
            this.posUpdateTime = Instant.parse(posUpdateTime);
        } else {
            this.posUpdateTime = Instant.ofEpochSecond(0);
        }
    }

    public String getBds40SeenTime() {
        return bds40SeenTime.toString();
    }

    public void setBds40SeenTime(String bds40SeenTime) {
        if (null != bds40SeenTime) {
            this.bds40SeenTime = Instant.parse(bds40SeenTime);
        } else {
            this.bds40SeenTime = Instant.ofEpochSecond(0);
        }
    }
}

@Getter
@Setter
class Droid {
    private String id, description;
}

@RestController
@RequestMapping("/droid")
class DroidController {
    private final Droid droid;

    public DroidController(Droid droid) {
        this.droid = droid;
    }

    @GetMapping
    Droid getDroid() {
        return droid;
    }
}

// @ConfigurationProperties: prefix 값으로 클래스 속성을 변경 가능하게 한다.
// 이때 setter가 존재해야 한다.
@Getter
@Setter
@ConfigurationProperties(prefix = "greeting")
class Greeting {
    private String name;
    private String coffee;
}

@RestController
@RequestMapping("/greeting")
class GreetingController {
    private final Greeting greeting; // Greeting Bean(빈)

    // 생성자
    public GreetingController(Greeting greeting) {
        this.greeting = greeting;
    }

    @GetMapping
    String getGreeting() {
        return greeting.getName();
    }

    @GetMapping("/coffee")
    String getCoffee() {
        return greeting.getCoffee();
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
