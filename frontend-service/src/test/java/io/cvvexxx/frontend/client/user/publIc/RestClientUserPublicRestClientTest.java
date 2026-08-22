package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestClientUserPublicRestClientTest {

    private MockRestServiceServer server;
    private RestClientUserPublicRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientUserPublicRestClient(builder.build());
    }

    private NewUserDto newUserDto() {
        return new NewUserDto("John", "Doe", "johndoe", "password", "john@mail.com", "MALE", LocalDate.of(1990, 1, 1));
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUserTests {

        @Test
        @DisplayName("при 400 выбрасывает BadRequestException с ошибками из тела ответа")
        void registerUser_When400_ShouldThrowBadRequestExceptionWithErrors() {
            // given
            server.expect(requestTo("http://localhost/api/users/register"))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"email is invalid\"]}"));
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());

            // when
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> client.registerUser(newUserDto(), avatar));

            // then
            assertEquals(List.of("email is invalid"), exception.getErrors());
        }

        @Test
        @DisplayName("при 409 с полем 'field' и 'detail' выбрасывает FieldAlreadyExistsException с этими данными")
        void registerUser_When409WithFieldAndDetail_ShouldThrowFieldAlreadyExistsExceptionWithThatData() {
            // given
            server.expect(requestTo("http://localhost/api/users/register"))
                    .andRespond(withStatus(CONFLICT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"detail\":\"Username already taken\",\"field\":\"username\"}"));
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());

            // when
            FieldAlreadyExistsException exception = assertThrows(FieldAlreadyExistsException.class,
                    () -> client.registerUser(newUserDto(), avatar));

            // then
            assertEquals("username", exception.getFieldName());
            assertEquals("Username already taken", exception.getMessage());
        }

        @Test
        @DisplayName("при 409 без тела ответа выбрасывает FieldAlreadyExistsException со значениями по умолчанию")
        void registerUser_When409WithEmptyBody_ShouldThrowFieldAlreadyExistsExceptionWithDefaults() {
            // given
            server.expect(requestTo("http://localhost/api/users/register"))
                    .andRespond(withStatus(CONFLICT));
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());

            // when
            FieldAlreadyExistsException exception = assertThrows(FieldAlreadyExistsException.class,
                    () -> client.registerUser(newUserDto(), avatar));

            // then
            assertEquals("usernameOrEmail", exception.getFieldName());
            assertEquals("Пользователь с такими данными уже существует", exception.getMessage());
        }
    }
}
