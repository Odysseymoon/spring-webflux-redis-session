package moon.odyssey.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moon.odyssey.entity.User;
import moon.odyssey.model.UserInfo;
import moon.odyssey.model.UserParam;
import moon.odyssey.repository.UserRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping(path = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserInfo> signUp(@RequestBody @Valid UserParam userParam) {

        return
            Mono.defer(() -> userRepository.findByUserId(userParam.getUserId()).map(Mono::just).orElseGet(Mono::empty))
                .subscribeOn(Schedulers.elastic())
                .flatMap(__ -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "The userId already exists : " + userParam.getUserId())))
                .switchIfEmpty(Mono.defer(() -> {
                        User newbie = new User();
                        newbie.setUserId(userParam.getUserId());
                        newbie.setPassword(userParam.getPassword());
                        return Mono.just(userRepository.save(newbie));
                    }).subscribeOn(Schedulers.elastic())
                )
                .cast(User.class)
                .map(user -> UserInfo.builder()
                                     .userId(user.getUserId())
                                     .build()
                )
            ;
    }

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
                                               .header("X-AUTH-TOKEN", session.getId())
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
