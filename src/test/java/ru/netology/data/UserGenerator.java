package ru.netology.data;

import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

import java.util.Locale;

@Data
public class UserGenerator {
    public static String generateLogin(String locale) {
        Faker faker = new Faker(Locale.forLanguageTag(locale));
        return faker.name().username();
    }

    public static String generatePass() {
        return "qwerty123";
    }

    public static class Registration {
        public static User generateUser(String locale) {
            return new User(generateLogin(locale), generatePass());
        }
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
