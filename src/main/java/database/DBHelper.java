package database;

import database.table.Session;
import database.table.Users;
import discord4j.core.object.entity.User;

import java.sql.*;

public class DBHelper {

    private static final String DB_NAME = "Database.db";

    private static Connection connection;

    public static boolean userExists(String id) throws SQLException {
        if(connection == null)
            connect();

        String qry = "SELECT 1 FROM " + Users.TABLE_NAME + " WHERE " + Users._ID + " = ?";
        PreparedStatement pStm = connection.prepareStatement(qry);
        pStm.setString(1, id);

        ResultSet rs = pStm.executeQuery();
        return rs.next();
    }

    public static Session.SessionStatus getSessionState(User user) throws SQLException {
        if (connection == null)
            connect();

        String qry = "SELECT " + Session.STATUS + " FROM " + Session.TABLE_NAME +
                " WHERE " + Session.DM_ID + " = ? AND" +
                " NOT " + Session.TABLE_NAME + "." + Session.STATUS + " = ?";

        PreparedStatement pStm = connection.prepareStatement(qry);
        pStm.setString(1, user.getId().asString());
        pStm.setInt(2, Session.SessionStatus.FINISHED.getVal());
        ResultSet rs = pStm.executeQuery();

        if (!rs.next())
            return Session.SessionStatus.INVALID;
        else
            return Session.SessionStatus.values()[rs.getInt(Session.STATUS)];
    }

    public static boolean hasActiveSession(User user) throws SQLException {
        return getSessionState(user) != Session.SessionStatus.INVALID;
    }

    public static boolean addUser(User user) throws SQLException {
        if(connection == null)
            connect();

        if(userExists(user.getId().asString()))
            return true;


        String qry = "INSERT INTO " + Users.TABLE_NAME + "(" +
                Users._ID + ", " +
                Users.USERNAME + ", " +
                Users.DISCRIMINATOR + ") VALUES(?, ?, ?)";
        PreparedStatement pStm = connection.prepareStatement(qry);
        pStm.setString(1, user.getId().asString());
        pStm.setString(2, user.getUsername());
        pStm.setString(3, user.getDiscriminator());
        pStm.execute();

        return false;
    }

    public static boolean createSession(User usr) throws SQLException {
        if(connection == null)
            connect();

        if(hasActiveSession(usr))
            return false;

        String qry = "INSERT INTO " + Session.TABLE_NAME + "(" +
                Session.DM_ID + ", " +
                Session.STATUS + ") VALUES(?, ?)";
        PreparedStatement pStm = connection.prepareStatement(qry);
        pStm.setString(1, usr.getId().asString());
        pStm.setInt(2, Session.SessionStatus.START.getVal());
        pStm.execute(); // TODO: Use return value.

        return true;
    }

    public static void createTables() {
        if(connection == null)
            connection = connect();

        try {
            Statement stm = connection.createStatement();
            stm.execute(Users.SQL_CREATE_TABLE);
            stm.execute(Session.SQL_CREATE_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void close() {
        try {
            if(connection != null)
                connection.close();
        } catch(SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private static Connection connect() {
        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
        } catch(SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace();
        }
        return connection;
    }

    public static boolean updatePlayerCount(User usr, int playerCount) throws SQLException {
        if(connection == null)
            connect();

        Session.SessionStatus status = getSessionState(usr);
        if(status != Session.SessionStatus.START)
            return false;

        String qry = "UPDATE " + Session.TABLE_NAME +
                " SET " + Session.PLAYER_COUNT + " = ? " +
                "WHERE " + Session.DM_ID + " = ? AND " +
                Session.STATUS + " = ?";
        PreparedStatement pStm = connection.prepareStatement(qry);
        pStm.setInt(1, playerCount);
        pStm.setString(2, usr.getId().asString());
        pStm.setInt(3, Session.SessionStatus.START.getVal());
        pStm.execute();

        return true;
    }
}
