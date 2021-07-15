package ru.netology.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
@AllArgsConstructor
@Getter
public class Card {
    String id;
    String number;
    Integer balance;
}
