package org.openhab.binding.openhasp.internal.layout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.openhab.binding.openhasp.internal.layout.handlebars.AssignHelper;
import org.openhab.binding.openhasp.internal.layout.handlebars.ClickObjectHelper;
import org.openhab.binding.openhasp.internal.layout.handlebars.IncHelper;
import org.openhab.binding.openhasp.internal.layout.handlebars.MathHelper;
import org.openhab.binding.openhasp.internal.layout.handlebars.SliderObjectHelper;
import org.openhab.binding.openhasp.internal.layout.handlebars.StatusLabelHelper;
import org.openhab.binding.openhasp.internal.layout.handlebars.ToIntHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.IfHelper;

//Handles sending all the layout to the HASP device

public class TemplateProcessor {
    private final static boolean DEBUG = false;
    // private final @NonNullByDefault({}) ClassLoader classLoader = TemplateProcessor.class.getClassLoader();
    private static final Logger logger = LoggerFactory.getLogger(TemplateProcessor.class);
    // private MustacheFactory mustacheFactory;
    Handlebars handlebars;

    public TemplateProcessor() {
        // Create a MustacheFactory
        // mustacheFactory = new DefaultMustacheFactory();

        // Create a Handlebars instance
        handlebars = new Handlebars();
        handlebars.registerHelper(IfHelper.NAME, IfHelper.INSTANCE);
        for (ConditionalHelpers helper : ConditionalHelpers.values()) {
            this.handlebars.registerHelper(helper.name(), helper);
        }
        handlebars.registerHelper("math", new MathHelper());
        handlebars.registerHelper(IncHelper.NAME, IncHelper.INSTANCE);
        handlebars.registerHelper(ToIntHelper.NAME, ToIntHelper.INSTANCE);
        handlebars.registerHelper(ClickObjectHelper.NAME, ClickObjectHelper.INSTANCE);
        handlebars.registerHelper(SliderObjectHelper.NAME, SliderObjectHelper.INSTANCE);
        handlebars.registerHelper("statusLabel", new StatusLabelHelper());
        handlebars.registerHelper(AssignHelper.NAME, AssignHelper.INSTANCE);
    }

    public String[] processTemplate(String name, Map<String, String> context) throws IOException {

        // Create a data context with the variables
        // Handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(new Object())
        // .combine("name", "John")
        // .combine("age", 30)
        // .combine("sum", sum)
        // .build();

        // Get the template
        Template template = handlebars.compile(name);

        // Render the template with the data context
        String renderedJson = template.apply(context);

        // Compile the template
        // Mustache mustache = mustacheFactory.compile(new InputStreamReader(classLoader.getResourceAsStream(name)),
        // name);
        // Get the template

        // Render the template with the data map
        // mustache.execute(writer, context).flush();

        logger.trace("TEMPLATE {}:", name);

        String[] lines = renderedJson.split("\n");
        ArrayList<String> cleanLines = new ArrayList<String>();

        if (DEBUG) {
            cleanLines.add("{\"comment\":\"START TEMPLATE:" + name + "\"}");
        }
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("{")) {
                cleanLines.add(line);
                logger.trace(line);
            } else {
                if (DEBUG && !line.isBlank()) {
                    cleanLines.add("{\"comment\":\"" + line + "\"}");
                }
            }
        }
        if (DEBUG) {
            cleanLines.add("{\"comment\":\"END TEMPLATE:" + name + "\"}");
        }

        // Output the rendered template

        // logger.trace(writer.toString());

        return cleanLines.toArray(new String[cleanLines.size()]);
    }
}
