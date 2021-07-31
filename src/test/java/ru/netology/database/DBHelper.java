package ru.netology.database;

import lombok.val;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import ru.netology.data.UserGenerator;

import java.sql.DriverManager;
import java.sql.SQLException;

public class DBHelper {

    public static String getAuthCode(UserGenerator.User user, String dbUrl) {
        val runner = new QueryRunner();
        val idSQL = "SELECT id FROM users WHERE login=?;";
        val dataSQL = "SELECT code FROM auth_codes WHERE user_id=? AND created=(select max(created) from auth_codes)";

        String authCode = null;
        try (
                val conn = DriverManager.getConnection(
                        dbUrl, "app", "pass"
                )
        ) {
            String userId = runner.query(conn, idSQL, new ScalarHandler<>(), user.getLogin());
            authCode = runner.query(conn, dataSQL, new ScalarHandler<>(), userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    return authCode;
    }
}
