package org.openhab.binding.openhasp.internal;

public class Util {
    public static String cleanString(String cadena) {
        cadena = cadena.trim();
        if (cadena.startsWith("\"")) {
            cadena = cadena.substring(1);
        }
        if (cadena.endsWith("\"")) {
            cadena = cadena.substring(0, cadena.length() - 1);
        }
        return cadena.trim();
    }
}
