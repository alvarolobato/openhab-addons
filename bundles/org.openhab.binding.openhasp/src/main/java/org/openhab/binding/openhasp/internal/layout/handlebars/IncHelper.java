package org.openhab.binding.openhasp.internal.layout.handlebars;

import java.io.IOException;
import java.util.HashMap;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Handlebars Inc Helper
 *
 * 
 * @see java.math.BigDecimal
 * @see java.math.MathContext
 */
public class IncHelper implements Helper<Object> {
    // private static final Logger logger = LoggerFactory.getLogger(IncHelper.class);

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new IncHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "inc";

    @Override
    public CharSequence apply(final Object value, Options options) throws IOException, IllegalArgumentException {
        // TODO Check number of parameter
        HashMap<Object, Object> realContext = options.data("root");
        Object val = realContext.get(value.toString());
        realContext.put(value.toString(), Integer.toString(Integer.parseInt(val.toString()) + 1));

        // logger.error("INC {}:{}", value, val);
        return val.toString();
    }
}
