package com.gordonlu.daffydialog.helpers;

import com.google.appinventor.components.common.OptionList;

import java.util.HashMap;
import java.util.Map;

public enum Font implements OptionList<String> {
    Default("DEFAULT"),
    Monospace("MONOSPACE"),
    SansSerif("SANS SERIF"),
    Serif("SERIF");

    private String font;

    Font(String f) {
        this.font = f;
    }

    public String toUnderlyingValue() {
        return font;
    }

    private static final Map<String, Font> lookup = new HashMap<>();

    static {
        for(Font f : Font.values()) {
        lookup.put(f.toUnderlyingValue(), f);
        }
    }

    public static Font fromUnderlyingValue(String f) {
        return lookup.get(f);
    }
}

