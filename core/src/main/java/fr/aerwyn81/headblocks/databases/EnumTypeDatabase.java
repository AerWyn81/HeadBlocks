package fr.aerwyn81.headblocks.databases;

public enum EnumTypeDatabase {
    SQLite, MySQL;

    static public EnumTypeDatabase Of(String t) {
        EnumTypeDatabase[] types = EnumTypeDatabase.values();
        for (EnumTypeDatabase type : types)
            if (type.name().equals(t))
                return type;
        return null;
    }
}
