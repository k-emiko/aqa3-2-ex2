package ru.netology.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

@Data
public class UserGenerator {

    public static User generateUser() {
        return new UserGenerator.User("vasya", "qwerty123");
    }

    @Value
    @AllArgsConstructor
    public static class User {
        String login;
        String password;
    }

    @Value
    @AllArgsConstructor
    public static class authInfo {
        String login;
        String code;
    }
}
