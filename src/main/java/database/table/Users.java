package database.table;

public class Users extends BaseCols {
    public static final String TABLE_NAME = "Users";

    public static final String USERNAME = "Username";
    public static final String DISCRIMINATOR = "Discriminator";

    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " +
                    TABLE_NAME +
                    "( " +
                    _ID + " INTEGER NOT NULL PRIMARY KEY, " +
                    USERNAME + " TEXT, " +
                    DISCRIMINATOR + " INTEGER"+
                    ")";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}
