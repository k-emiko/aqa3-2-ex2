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
import org.testcontainers.containers.GenericContainer;
import ru.netology.data.CardGenerator;
import ru.netology.data.Transfer;
import ru.netology.data.UserGenerator;

import java.lang.reflect.Type;
import java.util.List;

import static io.restassured.RestAssured.given;

public class APIHelper {
    private static String token;
    private static final String cardNumberAsterisks = "**** **** **** ";
    private static final String cardNumberPrefix = "5559 0000 0000 ";

    private static RequestSpecification requestSpec;

    public static void apiSetUp (GenericContainer appCont) {

        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://" + appCont.getHost())
                .setPort(appCont.getMappedPort(9999))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }

    public static void login(UserGenerator.User user) {
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

    public static void verification(UserGenerator.User user, String authCode) {
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
    }

    public static List<CardGenerator.Card> getCards() {
        List<CardGenerator.Card> cards;
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

        Type listType = new TypeToken<List<CardGenerator.Card>>() {
        }.getType();
        cards = gson.fromJson(response.path("$")
                        .toString()
                        .replace(cardNumberAsterisks, ""),
                listType);
        return cards;
    }

    public static int transferValidAccounts(List<CardGenerator.Card> initialCards, Integer amount) {
        Transfer transfer = new Transfer(
                cardNumberPrefix + initialCards.get(0).getNumber(),
                cardNumberPrefix + initialCards.get(1).getNumber(),
                amount);
        return transfer(token, transfer);
    }

    public static int transferInvalidAccounts(Integer amount, String to, String from) {
        Transfer transfer = new Transfer(cardNumberPrefix + from,
                cardNumberPrefix + to,
                amount);
        return transfer(token, transfer);
    }

    public static int transfer(String token, Transfer transfer) {
        Gson gson = new Gson();
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
}
