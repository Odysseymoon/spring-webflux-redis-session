package moon.odyssey.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moon.odyssey.model.UserInfo;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/test", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class TestController {

    @GetMapping(path = "/me")
    public Mono<UserInfo> testWithWebSession(WebSession session) {

        return
            Mono.justOrEmpty((UserInfo)session.getAttribute("user"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You don't have permission!")))
                .cast(UserInfo.class)
            ;
    }

    @GetMapping(path = "/me2")
    public Mono<UserInfo> testWithAttribute(@SessionAttribute("user") UserInfo user) {

        return
            Mono.justOrEmpty(user)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You don't have permission!")))
                .cast(UserInfo.class)
            ;
    }
}
