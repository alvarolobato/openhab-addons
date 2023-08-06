package org.openhab.binding.openhasp.internal.layout.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class ToIntHelper implements Helper<String> {
    /**
     * A singleton instance of this helper.
     */
    public static final Helper<String> INSTANCE = new ToIntHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "toInt";

    @Override
    public Object apply(String value, Options options) {
        // Use Integer.parseInt to convert the value from String to Integer
        return Integer.parseInt(value);
    }
}
