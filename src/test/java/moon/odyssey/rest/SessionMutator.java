package moon.odyssey.rest;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

public class SessionMutator implements WebTestClientConfigurer {

    private static Map<String, Object> sessionMap;

    private SessionMutator(final Map<String, Object> sessionMap) {
        this.sessionMap = Collections.unmodifiableMap(sessionMap);
    }

    public static SessionMutator sessionMutator(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return new SessionMutator(map);
    }

    @Override
    public void afterConfigurerAdded(final WebTestClient.Builder builder,
                                     final WebHttpHandlerBuilder httpHandlerBuilder,
                                     final ClientHttpConnector connector) {
        final SessionMutatorFilter sessionMutatorFilter = new SessionMutatorFilter();
        httpHandlerBuilder.filters(filters -> filters.add(0, sessionMutatorFilter));
    }

    private static class SessionMutatorFilter implements WebFilter {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain webFilterChain) {
            return exchange.getSession()
                           .doOnNext(webSession -> webSession.getAttributes().putAll(sessionMap))
                           .then(webFilterChain.filter(exchange));
        }
    }
}
