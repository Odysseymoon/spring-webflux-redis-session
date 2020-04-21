package moon.odyssey.rest;

import org.assertj.core.api.Assertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import lombok.extern.slf4j.Slf4j;
import moon.odyssey.model.UserInfo;

@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class TestControllerTest {

    @Autowired
    private WebTestClient testClient;

    @Test
    public void _0_init() {
        Assertions.assertThat(testClient).isNotNull();
    }

    @Test
    public void _1_testMe_withoutSession_should_return_400() {

        testClient
            .get()
            .uri("/api/test/me")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody(String.class)
            .consumeWith(result -> log.info("##### {}", result));
    }

    @Test
    public void _2_testMe_should_return_UserInfo() {

        testClient
            .mutateWith(SessionMutator.sessionMutator("user", UserInfo.builder().userId("testUser").build()))
            .get()
            .uri("/api/test/me")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserInfo.class)
            .consumeWith(result -> log.info("##### {}", result));
    }

    @Test
    public void _3_testMe2_withoutSession_should_return_400() {

        testClient
            .get()
            .uri("/api/test/me2")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody(String.class)
            .consumeWith(result -> log.info("##### {}", result));
    }

    @Test
    public void _4_testMe2_should_return_UserInfo() {

        testClient
            .mutateWith(SessionMutator.sessionMutator("user", UserInfo.builder().userId("testUser").build()))
            .get()
            .uri("/api/test/me2")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserInfo.class)
            .consumeWith(result -> log.info("##### {}", result));
    }

}