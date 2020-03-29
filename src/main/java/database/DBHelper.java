package database;

import database.table.Users;
import discord4j.core.object.entity.User;

import java.sql.*;

public class DBHelper {

    private static final String DB_NAME = "Database.db";

    private static Connection connection;

    public static boolean addUser(User user) throws SQLException {
        if(connection == null)
            connect();

        String qry = "SELECT 1 FROM " + Users.TABLE_NAME + " WHERE " +
                Users.USERNAME + " = ? AND " +
                Users.DISCRIMINATOR + " = ?";
        PreparedStatement pStm = connection.prepareStatement(qry);
        pStm.setString(1, user.getUsername());
        pStm.setInt(2, Integer.parseInt(user.getDiscriminator()));

        ResultSet rs = pStm.executeQuery();
        if(rs.next())
            return true;
        rs.close();

        qry = "INSERT INTO " + Users.TABLE_NAME + "(" +
                Users.USER_ID + ", " +
                Users.USERNAME + ", " +
                Users.DISCRIMINATOR + ") VALUES(?, ?, ?)";
        pStm = connection.prepareStatement(qry);
        pStm.setString(1, user.getId().asString());
        pStm.setString(2, user.getUsername());
        pStm.setString(3, user.getDiscriminator());
        pStm.execute();

        return false;
    }

    public static void createTables() {
        if(connection == null)
            connection = connect();

        try {
            Statement stm = connection.createStatement();
            stm.setQueryTimeout(30);
            stm.execute(Users.SQL_CREATE_TABLE);
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

            /*
            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");
            ResultSet rs = statement.executeQuery("select * from person");
            while(rs.next()) {
                // read the result set
                System.out.println("name = " + rs.getString("name"));
                System.out.println("id = " + rs.getInt("id"));
            }
             */
        } catch(SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace();
        }
        return connection;
    }
}
