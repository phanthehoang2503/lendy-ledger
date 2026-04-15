package com.lendy.app.utils;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;

public class CurrencyTextWatcher implements TextWatcher {
    private final TextInputEditText editText;
    private String current = "";

    public CurrencyTextWatcher(TextInputEditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (!s.toString().equals(current)) {
            editText.removeTextChangedListener(this);
            String cleanString = s.toString().replaceAll("[^\\d]", "");
            if (!cleanString.isEmpty()) {
                long parsed = Long.parseLong(cleanString);
                String formatted = FormatUtils.formatThousand(parsed);
                current = formatted;
                editText.setText(formatted);
                editText.setSelection(formatted.length());
            } else {
                current = "";
                editText.setText("");
            }
            editText.addTextChangedListener(this);
        }
    }
}
