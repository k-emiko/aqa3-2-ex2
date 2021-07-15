package ru.netology.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;


public class CardGenerator {

    public static String generateInvalidNumber() {
        return String.valueOf(Math.floor(Math.random() * (99999 - 1 + 1) + 1));
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
