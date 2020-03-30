package database.table;

import database.DBHelper;

import java.sql.*;

public class SessionTable extends BaseCols {

    public static final String PHASE_SETUP = "Setup";
    public static final String PHASE_VOTING = "Voting";
    public static final String PHASE_DONE = "Done";

    public static final String TABLE_NAME = "Session";

    public static final String DM_ID = "DmId";
    public static final String PLAYER_COUNT = "PlayerCount";
    public static final String PLAY_DATE = "PlayDate";
    public static final String PHASE = "Phase";

    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                    "( " +
                    _ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    DM_ID + " INTEGER NOT NULL, " +
                    PLAYER_COUNT + " INTEGER DEFAULT -1, " +
                    PLAY_DATE + " STRING, " +
                    PHASE + " TEXT NOT NULL, " +
                    "FOREIGN KEY (" + DM_ID + ") REFERENCES " +
                         UserTable.TABLE_NAME + "(" + UserTable._ID + ")" +
                    ")";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private String dmId;
    private int playerCount; //-1 indicates an invalid playerCount.
    private Date date;
    private String phase;

    private SessionTable(String dmId, int playerCount, Date date, String phase) {
        this.dmId = dmId;
        this.playerCount = playerCount;
        this.date = date;
        this.phase = phase;
    }

    /**
     * Creates a new session which is completely empty apart from DM-ID and PHASE
     * @param dmId The id of the session dm.
     * @return The inserted instance. Null if nothing was inserted.
     * @throws SQLException
     */
    public static SessionTable createSession(String dmId) throws SQLException {
        if(!UserTable.userWithIdExists(dmId))
            return null; //User does not exist.

        if(getActiveSession(dmId) != null)
            return null;

        SessionTable newEntry = new SessionTable(dmId, -1, null, PHASE_SETUP);
        insert(newEntry);

        return newEntry;
    }

    public static SessionTable getActiveSession(String dmId) throws SQLException {
        if(!UserTable.userWithIdExists(dmId))
            return null; //User does not exist.

        Connection conn = DBHelper.getConnection();
        String query = "SELECT " + PLAYER_COUNT + ", " + PLAY_DATE + ", " + PHASE +
                " FROM " + TABLE_NAME +
                " WHERE " + DM_ID + " = ? AND NOT " + PHASE + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, dmId);
        pStm.setString(2, PHASE_DONE);

        ResultSet rs = pStm.executeQuery();
        if(!rs.next())
            return null; //User doesnt have an active session.
        int playerCount = rs.getInt(PLAYER_COUNT);
        Date date = rs.getDate(PLAY_DATE);
        String phase = rs.getString(PHASE);
        rs.close();

        SessionTable res = new SessionTable(dmId, playerCount, date, phase);

        conn.close();
        return res;
    }

    private static void insert(SessionTable session) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "INSERT INTO " + SessionTable.TABLE_NAME + "(" +
                SessionTable.DM_ID + ", " +
                SessionTable.PLAYER_COUNT + ", " +
                SessionTable.PLAY_DATE + ", " +
                SessionTable.PHASE + ") VALUES(?, ?, ?, ?)";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, session.dmId);
        pStm.setInt(2, session.playerCount);
        pStm.setDate(3, session.date);
        pStm.setString(4, session.phase);
        pStm.execute();

        conn.close();
    }

    public String getPhase() {
        return phase;
    }

    public Integer getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "UPDATE " + TABLE_NAME +
                " SET " + PLAYER_COUNT + " = ? " +
                "WHERE " + DM_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setInt(1, playerCount);
        pStm.setString(2, dmId);
        pStm.execute();

        conn.close();

        this.playerCount = playerCount;
    }
}
