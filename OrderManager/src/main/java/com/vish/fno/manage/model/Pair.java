package com.vish.fno.manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class Pair<T, U> {
    T key;
    U value;
}
