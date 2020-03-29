package database.table;

public class Session extends BaseCols {

    public enum SessionStatus {
        START(0),
        PLAYER_COUNT_KNOWN(1),
        TIME_KNOWN(2),
        VOTING(3),
        FINISHED(4),
        INVALID(5);

        private int val;
        private SessionStatus(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public static final String TABLE_NAME = "Session";

    public static final String DM_ID = "DmId";
    public static final String PLAYER_COUNT = "PlayerCount";
    public static final String STATUS = "Status";
    public static final String PLAY_DATE = "PlayDate";

    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                    "( " +
                    _ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    DM_ID + " INTEGER NOT NULL, " +
                    PLAYER_COUNT + " INTEGER, " +
                    STATUS + " INTEGER, " +
                    PLAY_DATE + " DATE, " +
                    "FOREIGN KEY (" + DM_ID + ") REFERENCES " +
                         Users.TABLE_NAME + "(" + Users._ID + ")" +
                    ")";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}
