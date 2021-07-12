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
import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.data.Card;
import ru.netology.data.Transfer;
import ru.netology.data.UserGenerator;

import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransferTest {

    private final UserGenerator.User user = new UserGenerator.User("vasya", "qwerty123");
    String authCode;
    String token;
    List<Card> initialCards = new ArrayList<>();
    List<Card> actualCards = new ArrayList<>();
    String cardNumberAsterisks = "**** **** **** ";
    String cardNumberPrefix = "5559 0000 0000 ";
    Integer amount;
    int status;
    Integer expected;
    Integer actual;

    private static String dbUrl;
    static Network network = Network.newNetwork();

    @Rule
    public static MySQLContainer dbCont =
            (MySQLContainer) new MySQLContainer("mysql:latest")
                    .withDatabaseName("app")
                    .withUsername("app")
                    .withPassword("pass")
                    .withNetwork(network)
                    .withNetworkAliases("mysql")
                    .withFileSystemBind("./artifacts/init/schema.sql", "/docker-entrypoint-initdb.d/schema.sql", BindMode.READ_ONLY)
                    .withExposedPorts(3306);
    @Rule
    public static GenericContainer appCont =
            new GenericContainer(new ImageFromDockerfile("app-deadline")
                    .withDockerfile(Paths.get("artifacts/deadline/Dockerfile")))
                    .withEnv("TESTCONTAINERS_DB_USER", "app")
                    .withEnv("TESTCONTAINERS_DB_PASS", "pass")
                    .withExposedPorts(9999)
                    .withNetwork(network)
                    .withNetworkAliases("app-deadline");

    private static RequestSpecification requestSpec;

    @BeforeAll
    void setUpAll() throws SQLException {
        dbCont.start();
        dbUrl = dbCont.getJdbcUrl();
        appCont
                .withCommand("java -jar app-deadline.jar -P:jdbc.url=jdbc:mysql://mysql:3306/app")
                .start();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://" + appCont.getHost())
                .setPort(appCont.getMappedPort(9999))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();

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
        amount = 1000;
        int status = transferValidAccounts(amount);
        actualCards = getCards();
        expected = initialCards.get(0).getBalance() - amount;
        actual = actualCards.get(0).getBalance();
        assertEquals(expected, actual);
        assertEquals(400, status);
    }

    @Test
    public void transferOverdraft() {
        amount = 11000;
        status = transferValidAccounts(amount);
        assertEquals(200, status);
    }

    @Test
    public void transferNegative() {
        amount = -1000;
        status = transferValidAccounts(amount);
        assertEquals(200, status);
    }

    @Test
    public void transferFromInvalidAccount() {
        amount = 1000;
        status = transferInvalidAccounts("0", "0001", amount);
        assertEquals(200, status);
    }

    @Test
    public void transferToInvalidAccount() {
        amount = 1000;
        status = transferInvalidAccounts("0001", "0", amount);
        assertEquals(200, status);
    }

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
                        dbUrl, "app", "pass"
                )
        ) {
            String userId = runner.query(conn, idSQL, new ScalarHandler<>(), user.getLogin());
            authCode = runner.query(conn, dataSQL, new ScalarHandler<>(), userId);
        }
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
    }

    public List<Card> getCards() {
        List<Card> cards;
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
        return cards;
    }

    public int transferValidAccounts(Integer amount) {
        Transfer transfer = new Transfer(
                cardNumberPrefix + initialCards.get(0).getNumber(),
                cardNumberPrefix + initialCards.get(1).getNumber(),
                amount);
        return transfer(transfer);
    }

    public int transferInvalidAccounts(String from, String to, Integer amount) {
        Transfer transfer = new Transfer(cardNumberPrefix + from,
                cardNumberPrefix + to,
                amount);
        return transfer(transfer);
    }

    public int transfer(Transfer transfer) {
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
