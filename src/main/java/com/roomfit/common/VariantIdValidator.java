package com.roomfit.common;

import java.util.regex.Pattern;

public final class VariantIdValidator {

    private static final Pattern VALID_VARIANT_ID = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private VariantIdValidator() {
    }

    public static String validateNullable(String variantId) {
        if (variantId == null) {
            return null;
        }
        if (!VALID_VARIANT_ID.matcher(variantId).matches()) {
            throw new IllegalArgumentException(
                    "variantId must be null or contain lowercase letters, numbers, and single hyphens only");
        }
        return variantId;
    }
}
