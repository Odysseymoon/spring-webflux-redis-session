## Spring Boot WebFlux Redis Session Template
---

Spring Redis Session Template for WebFlux REST API with Header "X-AUTH-TOKEN"

### * Framework
- Java 1.8+
- Spring Boot 2.2.x
- Spring WebFlux
- Spring Data Redis Reactive
- Spring Session Data Redis
- Spring Data JPA
- MySQL 8.0.19 & Redis 5.0.8 with docker

### * How To Use

#### Docker 
```bash
~] cd docker
~] docker-compose up -d
```

#### Add Maven Dependency
- `pom.xml`
```xml
<dependencies>
    <!-- Spring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-data-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>

    <!--Common Pool -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
        <version>2.8.0</version>
    </dependency>

    <!-- Validator -->
    <dependency>
        <groupId>org.glassfish</groupId>
        <artifactId>javax.el</artifactId>
        <version>3.0.1-b09</version>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>${mysql.version}</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>

</dependencies>
```
#### Add properties at YAML
- `application.yaml`
```yaml
spring:
  profiles: local
  datasource:
    url: jdbc:mysql://localhost:13306/testDB?useUnicode=yes&characterEncoding=UTF-8
    username: testUser
    password: testPassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 16379
    password: redis12&*
    lettuce:
      pool:
        min-idle: 2
        max-idle: 5
        max-active: 10
  session:
    store-type: redis
```

#### Configure Spring Beans
- `Configuration`
```java
@SpringBootApplication
@EnableWebFlux
@EnableRedisRepositories
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 60*60*2)
public class SessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionApplication.class, args);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

        RedisSerializationContext<String, Object> serializationContext =
            RedisSerializationContext.<String, Object>newSerializationContext(new StringRedisSerializer())
                                     .hashKey(new StringRedisSerializer())
                                     .hashValue(new GenericJackson2JsonRedisSerializer())
                                     .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }

    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        HeaderWebSessionIdResolver sessionIdResolver = new HeaderWebSessionIdResolver();
        sessionIdResolver.setHeaderName("X-AUTH-TOKEN");        // Define Session Header Name
        return sessionIdResolver;
    }
}
```

#### Add Filter

Checking Session Attribute exsist or not

- `SessionFilter`
```java
@Slf4j
@Component
public class SessionFilter implements WebFilter {

    private PathPattern basePattern;

    private List<PathPattern> excludePatterns;

    public SessionFilter() {
        basePattern = new PathPatternParser()
                            .parse("/api/**");
        excludePatterns = new ArrayList<>();
        excludePatterns.add(new PathPatternParser().parse("/api/auth/sign*"));
        excludePatterns.add(new PathPatternParser().parse("/api/ping/**"));
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange serverWebExchange, final WebFilterChain webFilterChain) {


        ServerHttpRequest request = serverWebExchange.getRequest();
        log.info("{} : {} {}", request.getHeaders().getFirst("X-Forwarded-For") == null ? request.getRemoteAddress() : request.getHeaders().getFirst("X-Forwarded-For"), request.getMethodValue(), request.getURI().toString());

        if (basePattern.matches(request.getPath().pathWithinApplication())
            && !excludePatterns.stream()
                               .anyMatch(pathPattern -> pathPattern.matches(request.getPath().pathWithinApplication()))
        ) {

            return serverWebExchange.getSession()
                                    .doOnNext(session -> Optional.ofNullable(session.getAttribute("user"))
                                                                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not found session, Please Login again."))
                                    )
                                    .then(webFilterChain.filter(serverWebExchange));

        } else {

            return webFilterChain.filter(serverWebExchange);
        }
    }
}
```

#### Response SessionId with Header after SingIn
- `RestController`
```java
@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping(path = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserInfo>> signIn(@RequestBody @Valid UserParam userParam, WebSession session) {

        return
            Mono.defer(() -> userRepository.findByUserId(userParam.getUserId()).map(Mono::just).orElseGet(Mono::empty))
                .subscribeOn(Schedulers.elastic())
                .filter(user -> userParam.getPassword().equals(user.getPassword()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not found user info or invalid password")))
                .map(user -> UserInfo.builder()
                                     .userId(user.getUserId())
                                     .build()
                )
                .doOnNext(userInfo -> session.getAttributes().put("user", userInfo))
                .map(userInfo -> ResponseEntity.ok()
                                               .header("X-AUTH-TOKEN", session.getId())     //Add Header for Session
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .body(userInfo)
                )
            ;
    }

    @PutMapping(path = "/logout")
    public Mono<Void> logout(WebSession session) {

        return
            Mono.just(session)
                .flatMap(WebSession::invalidate)
            ;
    }

}
```

##### REST API with Session
- `RestController`
```java
@RestController
@RequestMapping(path = "/api/test", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class TestController {

    @GetMapping(path = "/me")
    public Mono<UserInfo> testWithWebSession(WebSession session) {      // Get Session by WebSession

        return
            Mono.justOrEmpty((UserInfo)session.getAttribute("user"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You don't have permission!")))
                .cast(UserInfo.class)
            ;
    }

    @GetMapping(path = "/me2")
    public Mono<UserInfo> testWithAttribute(@SessionAttribute("user") UserInfo user) {  // Get Session by @SessionAttribute

        return
            Mono.justOrEmpty(user)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You don't have permission!")))
                .cast(UserInfo.class)
            ;
    }
}
```

#### API call with Session Header
- `signin`
```bash
~] curl -v -X POST -H "Content-Type: application/json" -d '{"userId":"testUser", "password":"testPassword"}' http://localhost:8080/api/auth/signin
```

- API calls through `X-AUTH-TOKEN` Header in response
```bash
~] curl -X GET -H "X-AUTH-TOKEN: 1cbd4bd0-16b7-47c3-8b31-54ce32b88628" http://localhost:8080/api/test/me
```
