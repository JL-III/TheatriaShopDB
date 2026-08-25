package com.playtheatria.shopdb.database;

public class RegionRow {
    public Long id;
    public String name;
    public String server; // enum name, e.g. "THE_ARK"
    public int iX;
    public int iY;
    public int iZ;
    public int oX;
    public int oY;
    public int oZ;
    public Boolean active;
    public Long lastUpdated; // epoch millis, nullable
}
