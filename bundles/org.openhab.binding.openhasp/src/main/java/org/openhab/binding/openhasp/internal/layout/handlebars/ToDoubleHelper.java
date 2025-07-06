package org.openhab.binding.openhasp.internal.layout.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class ToDoubleHelper implements Helper<String> {
    /**
     * A singleton instance of this helper.
     */
    public static final Helper<String> INSTANCE = new ToDoubleHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "toDouble";

    @Override
    public Object apply(String value, Options options) {
        return Double.parseDouble(value);
    }
}
