package fr.aerwyn81.headblocks.data.head;

public record CatalogEntry(String type, String value, String raw) {

    public static CatalogEntry parse(Object raw) {
        if (!(raw instanceof String text)) {
            return null;
        }

        var separator = text.indexOf(':');
        if (separator <= 0) {
            return null;
        }

        return new CatalogEntry(text.substring(0, separator).trim().toLowerCase(), text.substring(separator + 1).trim(), text);
    }
}
