package org.openhab.binding.openhasp.internal.layout.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.openhasp.internal.layout.TemplateProcessor;
import org.openhab.binding.openhasp.internal.mapping.IObjItemMapping;

@NonNullByDefault
public interface IComponent extends IObjItemMapping {

    void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException;

    void updateState();

    void sendStatusUpdate(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException;

    String getComponent();

    int getHeight();
}
