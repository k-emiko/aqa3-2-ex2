package ru.netology.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

@Data
public class UserGenerator {

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
