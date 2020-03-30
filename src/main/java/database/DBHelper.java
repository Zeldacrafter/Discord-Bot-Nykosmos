package database;

import database.table.VoteTable;
import database.table.SessionTable;
import database.table.UserTable;

import java.sql.*;

public class DBHelper {

    private static final String DB_NAME = "Database.db";

    public static void createTables() {
        Connection connection = getConnection();

        try {
            Statement stm = connection.createStatement();
            stm.execute(UserTable.SQL_CREATE_TABLE);
            stm.execute(SessionTable.SQL_CREATE_TABLE);
            stm.execute(VoteTable.SQL_CREATE_TABLE);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
