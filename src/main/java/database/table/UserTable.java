package database.table;

import database.DBHelper;
import discord4j.core.object.entity.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserTable extends BaseCols {
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

    private String id;
    private String username;
    private int discriminator;

    private UserTable(String id, String username, int discriminator) {
        this.id = id;
        this.username = username;
        this.discriminator = discriminator;
    }

    public static boolean userWithIdExists(String id) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String qry = "SELECT 1 FROM " + UserTable.TABLE_NAME + " WHERE " + UserTable._ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(qry);
        pStm.setString(1, id);

        ResultSet rs = pStm.executeQuery();
        boolean foundEntry = rs.next();
        rs.close();
        pStm.close();
        conn.close();

        return foundEntry;
    }

    /**
     * Inserts a new user into the database
     * @param user The user object that holds the data we want to insert.
     * @return Instance of the inserted user, null if nothing was inserted.
     */
    public static UserTable insert(User user) throws SQLException {
        if(userWithIdExists(user.getId().asString()))
            return null;

        Connection conn = DBHelper.getConnection();

        String query = "INSERT INTO " + TABLE_NAME + " VALUES(?, ?, ?)";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, user.getId().asString());
        pStm.setString(2, user.getUsername());
        pStm.setString(3, user.getDiscriminator());
        pStm.execute();
        pStm.close();

        conn.close();

        return new UserTable(user.getId().asString(),
                         user.getUsername(),
                         Integer.parseInt(user.getDiscriminator()));
    }
}
