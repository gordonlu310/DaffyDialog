package com.gordonlu.daffydialog.helpers;

import com.google.appinventor.components.common.OptionList;

import java.util.*;

public enum InputType implements OptionList<Integer> {
    EmailAddress(32),
    Text(1),
    Number(2),
    PhoneNumber(3);

    private int inputType;

    InputType(int i) {
        this.inputType = i;
    }

    public Integer toUnderlyingValue() {
        return inputType;
    }

    private static final Map<Integer, InputType> lookup = new HashMap<>();

    static {
        for(InputType i : InputType.values()) {
        lookup.put(i.toUnderlyingValue(), i);
        }
    }

    public static InputType fromUnderlyingValue(int i) {
        return lookup.get(i);
    }
}

