package ru.netology.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;


public class CardGenerator {

    public static String generateInvalidNumber() {
        return String.valueOf((int)Math.floor(Math.random() * (9999 - 1000 + 1) + 1000));
    }

    public static String generate17DigitNumber() {
        return String.valueOf((int)Math.floor(Math.random() * (99999 - 10000 + 1) + 10000));
    }

    @Value
    @AllArgsConstructor
    @Getter
    public
    class Card {
        String id;
        String number;
        Integer balance;

    }

}
