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
public class ObjectIdHelper implements Helper<Object> {
    // private static final Logger logger =
    // LoggerFactory.getLogger(IncHelper.class);

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new ObjectIdHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "objId";

    @Override
    public Object apply(final Object value, Options options) throws IOException, IllegalArgumentException {
        HashMap<Object, Object> realContext = options.data("root");
        String objId = HBUtils.getObjId(realContext);
        realContext.put(OpenHASPLayout.OBJECT_ID + value.toString(), objId);
        return "";
    }
}
