package ru.netology.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
@AllArgsConstructor
@Getter
public class Card {
    private String id;
    private String number;
    private Integer balance;
}
