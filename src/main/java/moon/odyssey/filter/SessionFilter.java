package moon.odyssey.filter;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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
