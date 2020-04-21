package moon.odyssey.rest;

import org.assertj.core.api.Assertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import moon.odyssey.entity.User;
import moon.odyssey.model.UserInfo;
import moon.odyssey.model.UserParam;
import moon.odyssey.repository.UserRepository;

@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class AuthControllerTest {

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private UserRepository repository;

    @Test
    public void _0_init() {
        Assertions.assertThat(testClient).isNotNull();
    }

    @Test
    public void _1_singup_should_return_OK() {

        Mockito.when(repository.findByUserId(Mockito.anyString()))
               .thenReturn(Optional.empty());

        User testUser = new User("testUser", "testPassword");

        Mockito.when(repository.save(testUser))
               .thenReturn(testUser);

        UserParam param = new UserParam("testUser", "testPassword");

        testClient
            .post()
            .uri("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(param)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(UserInfo.class)
            .consumeWith(result -> log.info("##### {}", result));
    }

    @Test
    public void _2_singup_duplication_should_return_409() {

        User testUser = new User("testUser", "testPassword");

        Mockito.when(repository.findByUserId(Mockito.anyString()))
               .thenReturn(Optional.of(testUser));

        UserParam param = new UserParam("testUser", "testPassword");

        testClient
            .post()
            .uri("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(param)
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(String.class)
            .consumeWith(result -> log.info("##### {}", result));
    }

    @Test
    public void _3_singin_should_return_AuthHeader() {

        User testUser = new User("testUser", "testPassword");

        Mockito.when(repository.findByUserId(Mockito.anyString()))
               .thenReturn(Optional.of(testUser));

        UserParam param = new UserParam("testUser", "testPassword");

        testClient
            .post()
            .uri("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(param)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-AUTH-TOKEN")
            .expectBody(UserInfo.class)
            .consumeWith(result -> log.info("##### {}", result));

    }

    @Test
    public void _5_singin_NotFoundUser_should_return_400() {

        Mockito.when(repository.findByUserId(Mockito.anyString()))
               .thenReturn(Optional.empty());

        UserParam param = new UserParam("testUser", "testPassword");

        testClient
            .post()
            .uri("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(param)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .consumeWith(result -> log.info("##### {}", result));

    }

    @Test
    public void _5_singin_InvalidUser_should_return_400() {

        User testUser = new User("testUser", "testPassword");

        Mockito.when(repository.findByUserId(Mockito.anyString()))
               .thenReturn(Optional.of(testUser));

        UserParam param = new UserParam("testUser", "testPassword2");

        testClient
            .post()
            .uri("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(param)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .consumeWith(result -> log.info("##### {}", result));

    }

    @Test
    public void _6_logout_should_return_OK() {

        testClient
            .mutateWith(SessionMutator.sessionMutator("user", UserInfo.builder().userId("testUser").build()))
            .put()
            .uri("/api/auth/logout")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(result -> log.info("##### {}", result));
    }


}