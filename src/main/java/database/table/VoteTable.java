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

    public static final String SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                "( " +
                _ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                SESSION_ID + " INTEGER NOT NULL, " +
                PLAYER_ID + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + SESSION_ID + ") REFERENCES " +
                    SessionTable.TABLE_NAME + "(" + SessionTable._ID + "), " +
                "FOREIGN KEY (" + PLAYER_ID + ") REFERENCES " +
                    UserTable.TABLE_NAME + "(" + UserTable._ID + ")" +
                ")";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private int sessionId;
    private int playerId;

    private VoteTable(int sessionId, int playerId) {
        this.sessionId = sessionId;
        this.playerId = playerId;
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
                SESSION_ID + ", " + PLAYER_ID + ") VALUES(?, ?)";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setInt(1, sessionId);
        pStm.setString(2, playerId);
        pStm.execute();

        conn.close();
    }

    public static ArrayList<UserTable> getVotes(int sessionId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "SELECT " + PLAYER_ID + " FROM " + TABLE_NAME + " WHERE " + SESSION_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setInt(1, sessionId);
        ResultSet rs = pStm.executeQuery();

        ArrayList<UserTable> res = new ArrayList<>();
        while(rs.next()) {
            String playerId = rs.getString(PLAYER_ID);
            UserTable player = UserTable.getUserWithId(playerId);
            if(player == null) //should never happen.
                continue;
            res.add(player);
        }

        conn.close();
        return res;
    }
}
