package ru.netology;

import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.data.CardGenerator;
import ru.netology.data.UserGenerator;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.netology.APIHelper.getCards;
import static ru.netology.DBHelper.getAuthCode;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransferTest {

    private final UserGenerator.User user = UserGenerator.generate();
    List<CardGenerator.Card> initialCards = new ArrayList<>();
    List<CardGenerator.Card> actualCards = new ArrayList<>();
    Integer amount;
    int status;
    Integer expected;
    Integer actual;

    static Network network = Network.newNetwork();

    @Rule
    private static final MySQLContainer dbCont =
            new MySQLContainer("mysql:latest");
    @Rule
    private static final GenericContainer appCont =
            new GenericContainer(new ImageFromDockerfile("app-deadline")
                    .withDockerfile(Paths.get("artifacts/deadline/Dockerfile")));

    @BeforeAll
    void setUpContainersAndAPI() throws SQLException {
        dbCont
                .withDatabaseName("app")
                .withUsername("app")
                .withPassword("pass")
                .withNetwork(network)
                .withNetworkAliases("mysql")
                .withFileSystemBind("./artifacts/init/schema.sql", "/docker-entrypoint-initdb.d/schema.sql", BindMode.READ_ONLY)
                .withExposedPorts(3306)
                .start();
        String dbUrl = dbCont.getJdbcUrl();
        appCont
                .withEnv("TESTCONTAINERS_DB_USER", "app")
                .withEnv("TESTCONTAINERS_DB_PASS", "pass")
                .withExposedPorts(9999)
                .withNetwork(network)
                .withNetworkAliases("app-deadline")
                .withCommand("java -jar app-deadline.jar -P:jdbc.url=jdbc:mysql://mysql:3306/app")
                .start();
        APIHelper.apiSetUp(appCont);
        APIHelper.login(user);
        String authCode = getAuthCode(user, dbUrl);
        APIHelper.verification(user, authCode);
    }

    @BeforeEach
    public void setUpEach() {
        initialCards = getCards();
    }

    @Test
    public void transferPositive() {
        amount = 1000;
        status = APIHelper.transferValidAccounts(initialCards,amount);
        actualCards = getCards();
        expected = initialCards.get(0).getBalance() - amount;
        actual = actualCards.get(0).getBalance();
        assertAll(
                () -> assertEquals(expected, actual),
                () -> assertEquals(200, status));
    }

    @ParameterizedTest(name = "{arguments}")
    @CsvFileSource(resources = "/NegativeTransferTestValidAccounts.csv", delimiter = '|', numLinesToSkip = 1)
    public void negativeTestValidAccounts (String test, Integer amount) {
        status = APIHelper.transferValidAccounts(initialCards, amount);
        actualCards = getCards();
        expected = initialCards.get(0).getBalance();
        actual = actualCards.get(0).getBalance();
        assertAll(
                () -> assertEquals(400, status),
                () -> assertEquals(expected, actual));
    }

    @Test @DisplayName("From invalid account")
    public void negativeTestFromInvalid () {
        amount = 1000;
        status = APIHelper.transferInvalidAccounts(amount, initialCards.get(1).getNumber(), CardGenerator.generateInvalidNumber());
        expected = initialCards.get(1).getBalance();
        actualCards = getCards();
        actual = actualCards.get(1).getBalance();
        assertAll(
                () -> assertEquals(400, status),
                () -> assertEquals(expected, actual));
    }

    @Test @DisplayName("To invalid account")
    public void negativeTestToInvalid () {
        amount = 1000;
        status = APIHelper.transferInvalidAccounts(amount, CardGenerator.generateInvalidNumber(), initialCards.get(1).getNumber());
        expected = initialCards.get(1).getBalance();
        actualCards = getCards();
        actual = actualCards.get(1).getBalance();
        assertAll(
                () -> assertEquals(400, status),
                () -> assertEquals(expected, actual));
    }

    @Test @DisplayName("Incorrect number format")
    public void negative17DigitNumber () {
        amount = 1000;
        status = APIHelper.transferInvalidAccounts(amount, CardGenerator.generate17DigitNumber(), initialCards.get(1).getNumber());
        expected = initialCards.get(1).getBalance();
        actualCards = getCards();
        actual = actualCards.get(1).getBalance();
        assertAll(
                () -> assertEquals(400, status),
                () -> assertEquals(expected, actual));
    }
}
