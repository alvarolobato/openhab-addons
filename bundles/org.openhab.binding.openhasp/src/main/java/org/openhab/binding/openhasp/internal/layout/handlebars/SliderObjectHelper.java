package org.openhab.binding.openhasp.internal.layout.handlebars;

import java.io.IOException;
import java.util.HashMap;

import org.openhab.binding.openhasp.internal.layout.OpenHASPLayout;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Handlebars Inc Helper
 *
 */
public class SliderObjectHelper implements Helper<Object> {
    // private static final Logger logger =
    // LoggerFactory.getLogger(IncHelper.class);

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new SliderObjectHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "sliderObj";

    @Override
    public Object apply(final Object value, Options options) throws IOException, IllegalArgumentException {
        HashMap<Object, Object> realContext = options.data("root");
        String objId = HBUtils.getObjId(realContext);
        realContext.put(OpenHASPLayout.SLIDE_OBJECT_ID, objId);
        return "";
    }
}
