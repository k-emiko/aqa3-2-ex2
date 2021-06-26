package ru.netology;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.val;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.data.Card;
import ru.netology.data.Transfer;
import ru.netology.data.UserGenerator;

import java.lang.reflect.Type;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginTest {

    private final UserGenerator.User user = new UserGenerator.User("vasya", "qwerty123");
    String authCode;
    String token;
    List<Card> initialCards = new ArrayList<>();
    List<Card> actualCards = new ArrayList<>();
    String cardNumberAsterisks = "**** **** **** ";
    String cardNumberPrefix = "5559 0000 0000 ";
    private final RequestSpecification requestSpec = new RequestSpecBuilder()
            .setBaseUri("http://localhost")
            .setPort(9999)
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .log(LogDetail.ALL)
            .build();

    public void login() {
        Gson gson = new Gson();
        RestAssured.defaultParser = Parser.JSON;
        given()
                .spec(requestSpec)
                .body(gson.toJson(user))
                .when()
                .post("/api/auth")
                .then()
                .statusCode(200);
    }

    public void getAuthCode() throws SQLException {
        val runner = new QueryRunner();
        val idSQL = "SELECT id FROM users WHERE login=?;";
        val dataSQL = "SELECT code FROM auth_codes WHERE user_id=? AND created=(select max(created) from auth_codes)";

        try (
                val conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/app", "app", "pass"
                )
        ) {
            String userId = runner.query(conn, idSQL, new ScalarHandler<>(), user.getLogin());
            authCode = runner.query(conn, dataSQL, new ScalarHandler<>(), userId);
        }
        //System.out.println(authCode);
    }

    public void verification() {
        Gson gson = new Gson();
        UserGenerator.authInfo authCodeJson = new UserGenerator.authInfo(user.getLogin(), authCode);
        token = given()
                .spec(requestSpec)
                .body(gson.toJson((authCodeJson)))
                .when()
                .post("/api/auth/verification")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
        //System.out.println("token: " + token);
    }

    public List<Card> getCards() {
        List<Card> cards = new ArrayList<>();
        Gson gson = new Gson();
        Response response =
                given()
                        .spec(requestSpec)
                        .auth()
                        .preemptive()
                        .oauth2(token)
                        .when()
                        .get("/api/cards")
                        .then()
                        .extract()
                        .response();

        Type listType = new TypeToken<List<Card>>() {
        }.getType();
        cards = gson.fromJson(response.path("$")
                        .toString()
                        .replace(cardNumberAsterisks, ""),
                listType);

        //how to address: System.out.println(cards.get(0).getId());
        //System.out.println("cards: " + cards);
        return cards;
    }

    public int transferValidAccounts(Integer amount) {
        Gson gson = new Gson();
        Transfer transfer = new Transfer(cardNumberPrefix + initialCards.get(0).getNumber(),
                cardNumberPrefix + initialCards.get(1).getNumber(),
                amount);

        return given()
                .spec(requestSpec)
                .auth()
                .preemptive()
                .oauth2(token)
                .body(gson.toJson(transfer))
                .when()
                .post("/api/transfer")
                .then()
                .extract()
                .statusCode();
    }

    public int transferInvalidAccounts(String from, String to, Integer amount) {
        Gson gson = new Gson();
        Transfer transfer = new Transfer(cardNumberPrefix + from,
                cardNumberPrefix + to,
                amount);

        return given()
                .spec(requestSpec)
                .auth()
                .preemptive()
                .oauth2(token)
                .body(gson.toJson(transfer))
                .when()
                .post("/api/transfer")
                .then()
                .extract()
                .statusCode();
    }

    @BeforeAll
    public void setUpAll() throws SQLException {
        login();
        getAuthCode();
        verification();
    }

    @BeforeEach
    public void setUpEach() {
        initialCards = getCards();
    }

    @Test
    public void transferPositive() {
        Integer amount = 1000;
        int status = transferValidAccounts(amount);
        actualCards = getCards();
        Integer expected = initialCards.get(0).getBalance() - amount;
        Integer actual = actualCards.get(0).getBalance();
        assertEquals(expected, actual);
        assertEquals(200, status);
    }

    @Test
    public void transferOverdraft() {
        Integer amount = 11000;
        int status = transferValidAccounts(amount);
        assertEquals(400, status);
    }

    @Test
    public void transferNegative() {
        Integer amount = -1000;
        int status = transferValidAccounts(amount);
        assertEquals(400, status);
    }

    @Test
    public void transferFromInvalidAccount() {
        Integer amount = 1000;
        int status = transferInvalidAccounts("0", "0001", amount);
        assertEquals(400, status);
    }

    @Test
    public void transferToInvalidAccount() {
        Integer amount = 1000;
        int status = transferInvalidAccounts("0001", "0", amount);
        assertEquals(400, status);
    }

}
