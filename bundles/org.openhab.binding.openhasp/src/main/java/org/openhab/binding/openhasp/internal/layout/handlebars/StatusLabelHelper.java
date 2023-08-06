package org.openhab.binding.openhasp.internal.layout.handlebars;

import java.io.IOException;
import java.util.HashMap;

import org.openhab.binding.openhasp.internal.layout.OpenHASPLayout;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Handlebars Inc Helper
 *
 * 
 */
public class StatusLabelHelper implements Helper<Object> {
    // private static final Logger logger = LoggerFactory.getLogger(IncHelper.class);

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> StatusLabelHelper = new StatusLabelHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "statusLabel";

    @Override
    public CharSequence apply(final Object value, Options options) throws IOException, IllegalArgumentException {
        // logger.error("HELPER: Parameter {} class {}", value, value.getClass().getName());
        HashMap<Object, Object> realContext = options.data("root");
        String objId = HBUtils.getObjId(realContext);
        realContext.put(OpenHASPLayout.STATUS_LABEL_ID, objId);
        return "";
    }
}
