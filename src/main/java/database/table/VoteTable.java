package database.table;

import database.DBHelper;
import org.sqlite.core.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class VoteTable extends BaseCols {
    public static final String TABLE_NAME = "Vote";

    public static final String SESSION_ID = "SessionId";
    public static final String PLAYER_ID = "PlayerId";
    public static final String STATUS = "Status";

    public static final String SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                "( " +
                _ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                SESSION_ID + " INTEGER NOT NULL, " +
                PLAYER_ID + " INTEGER NOT NULL, " +
                STATUS + " TEXT NOT NULL, " +
                "FOREIGN KEY (" + SESSION_ID + ") REFERENCES " +
                    SessionTable.TABLE_NAME + "(" + SessionTable._ID + "), " +
                "FOREIGN KEY (" + PLAYER_ID + ") REFERENCES " +
                    UserTable.TABLE_NAME + "(" + UserTable._ID + ")" +
                ")";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static final String STATUS_UNDECIDED = "Undecided";
    public static final String STATUS_ACCEPTED = "Accepted";
    public static final String STATUS_DECLINED = "Declined";

    private int sessionId;
    private String playerId;
    private String status;

    private VoteTable(int sessionId, String playerId, String status) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.status = status;
    }

    public static void delete(int sessionId, String playerId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "DELETE FROM " + TABLE_NAME + " WHERE " +
                SESSION_ID + " = ? AND " + PLAYER_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setInt(1, sessionId);
        pStm.setString(2, playerId);
        pStm.execute();

        conn.close();
    }

    public static void insert(int sessionId, String playerId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "INSERT INTO " + TABLE_NAME + "(" +
                SESSION_ID + ", " + PLAYER_ID + ", " + STATUS + ") VALUES(?, ?, ?)";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setInt(1, sessionId);
        pStm.setString(2, playerId);
        pStm.setString(3, STATUS_UNDECIDED);
        pStm.execute();

        conn.close();
    }

    public static ArrayList<VoteTable> getVotes(int sessionId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "SELECT " + PLAYER_ID + ", " + STATUS + " FROM " + TABLE_NAME +
                " WHERE " + SESSION_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setInt(1, sessionId);

        ResultSet rs = pStm.executeQuery();
        ArrayList<VoteTable> res = new ArrayList<>();
        while(rs.next()) {
            String playerId = rs.getString(PLAYER_ID);
            String status = rs.getString(STATUS);
            res.add(new VoteTable(sessionId, playerId, status));
        }
        conn.close();

        return res;
    }

    public void setStatus(String status) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "UPDATE " + TABLE_NAME +
                " SET " + STATUS + " = ?" +
                " WHERE " + SESSION_ID + " = ? AND " + PLAYER_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, status);
        pStm.setInt(2, sessionId);
        pStm.setString(3, playerId);
        pStm.execute();

        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getPlayerId() {
        return playerId;
    }
}
