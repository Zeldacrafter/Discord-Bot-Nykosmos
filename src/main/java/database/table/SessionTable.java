package database.table;

import database.DBHelper;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

import static java.time.temporal.ChronoUnit.DAYS;

public class SessionTable extends BaseCols {

    public static final String PHASE_SETUP = "Setup";
    public static final String PHASE_VOTING = "Voting";
    public static final String PHASE_DONE = "Done";

    public static final String TABLE_NAME = "Session";

    public static final String DM_ID = "DmId";
    public static final String PLAYER_COUNT = "PlayerCount";
    public static final String PLAY_DATE = "PlayDate";
    public static final String PHASE = "Phase";
    public static final String DM_COMMENT = "DmComment";
    public static final String VOTE_MESSAGE_ID = "VoteMessageId";

    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                    "( " +
                    _ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    DM_ID + " INTEGER NOT NULL, " +
                    PLAYER_COUNT + " INTEGER DEFAULT -1, " +
                    PLAY_DATE + " STRING, " +
                    PHASE + " TEXT NOT NULL, " +
                    DM_COMMENT + " TEXT, " +
                    VOTE_MESSAGE_ID + " INTEGER, " +
                    "FOREIGN KEY (" + DM_ID + ") REFERENCES " +
                         UserTable.TABLE_NAME + "(" + UserTable._ID + ")" +
                    ")";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private int id;
    private String dmId;
    private int playerCount; //-1 indicates an invalid playerCount.
    private Date date;
    private String phase;
    private String dmComment;
    private String voteMsgId;
    private Date voteUntil;

    private SessionTable(int id, String dmId, int playerCount, Date date,
                         String phase, String dmComment, String voteMsgId) {
        this.id = id;
        this.dmId = dmId;
        this.playerCount = playerCount;
        this.date = date;
        this.phase = phase;
        this.dmComment = dmComment;
        this.voteMsgId = voteMsgId;
    }

    /**
     * Creates a new session which is completely empty apart from DM-ID and PHASE
     * @param dmId The id of the session dm.
     * @return The inserted instance. Null if nothing was inserted.
     * @throws SQLException
     */
    public static SessionTable createSession(String dmId) throws SQLException {
        if(!UserTable.userWithIdExists(dmId)) {
            System.out.println("Could not create session because user with id " + dmId + " is not in database.");
            return null; //User does not exist.
        }

        if(getActiveSession(dmId) != null) {
            System.out.println("Could not create session because user with id " + dmId + " already has an active session.");
            return null;
        }

        SessionTable newEntry = new SessionTable(-1, dmId, -1, null, PHASE_SETUP, null, null);
        insert(newEntry);

        return newEntry;
    }

    public static SessionTable getActiveSession(String dmId) throws SQLException {
        if(!UserTable.userWithIdExists(dmId))
            return null; //User does not exist.

        Connection conn = DBHelper.getConnection();
        String query = "SELECT " + _ID + ", " + PLAYER_COUNT + ", " + PLAY_DATE + ", " +
                                   PHASE + ", " + DM_COMMENT + ", " +
                                   VOTE_MESSAGE_ID +
                " FROM " + TABLE_NAME +
                " WHERE " + DM_ID + " = ? AND NOT " + PHASE + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, dmId);
        pStm.setString(2, PHASE_DONE);

        ResultSet rs = pStm.executeQuery();
        if(!rs.next())
            return null; //User doesnt have an active session.
        int id = rs.getInt(_ID);
        int playerCount = rs.getInt(PLAYER_COUNT);
        Date date = rs.getDate(PLAY_DATE);
        String phase = rs.getString(PHASE);
        String dmComment = rs.getString(DM_COMMENT);
        String voteMsgId = rs.getString(VOTE_MESSAGE_ID);
        rs.close();

        SessionTable res = new SessionTable(id, dmId, playerCount, date, phase, dmComment, voteMsgId);

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

    public static SessionTable getFromVoteMsgId(String msgId) throws SQLException {
        Connection conn = DBHelper.getConnection();
        String query = "SELECT " + _ID + ", " + DM_ID + ", " + PLAYER_COUNT + ", " + PLAY_DATE + ", " +
                PHASE + ", " + DM_COMMENT +
                " FROM " + TABLE_NAME +
                " WHERE " + VOTE_MESSAGE_ID + " = ? AND " + PHASE + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, msgId);
        pStm.setString(2, PHASE_VOTING);

        ResultSet rs = pStm.executeQuery();

        if(!rs.next())
            return null; //Message has no active voting.
        int id = rs.getInt(_ID);
        String dmId = rs.getString(DM_ID);
        int playerCount = rs.getInt(PLAYER_COUNT);
        Date date = rs.getDate(PLAY_DATE);
        String phase = rs.getString(PHASE);
        String dmComment = rs.getString(DM_COMMENT);
        rs.close();

        SessionTable res = new SessionTable(id, dmId, playerCount, date, phase,
                              dmComment, msgId);

        conn.close();
        return res;
    }

    /**
     * Deletes the current session (if it is in setup phase).
     * @return true if an entry was deleted, false otherwise.
     * @throws SQLException
     */
    public boolean deleteActiveSession() throws SQLException {

        if(!PHASE_SETUP.equals(phase)) {
            System.out.println("delteActiveSession: Could not delete session with" +
                    "phase " + phase);
            return false;
        }

        Connection conn = DBHelper.getConnection();

        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + DM_ID + " = ? AND " + PHASE + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, dmId);
        pStm.setString(2, phase);
        pStm.execute();

        conn.close();
        return true;
    }

    public String getPhase() {
        return phase;
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

    public void setDmComment(String comment) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "UPDATE " + TABLE_NAME +
                " SET " + DM_COMMENT + " = ? " +
                "WHERE " + DM_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, comment);
        pStm.setString(2, dmId);
        pStm.execute();

        conn.close();

        dmComment = comment;
    }

    public void setDate(Date date) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "UPDATE " + TABLE_NAME +
                " SET " + PLAY_DATE + " = ? " +
                "WHERE " + DM_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setDate(1, date);
        pStm.setString(2, dmId);
        pStm.execute();

        conn.close();

        this.date = date;
    }

    public void setPhase(String phase) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "UPDATE " + TABLE_NAME +
                " SET " + PHASE + " = ? " +
                "WHERE " + DM_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, phase);
        pStm.setString(2, dmId);
        pStm.execute();

        conn.close();

        this.phase = phase;
    }

    public void setVoteMsgId(String msgId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        String query = "UPDATE " + TABLE_NAME +
                " SET " + VOTE_MESSAGE_ID + " = ? " +
                "WHERE " + DM_ID + " = ?";
        PreparedStatement pStm = conn.prepareStatement(query);
        pStm.setString(1, msgId);
        pStm.setString(2, dmId);
        pStm.execute();

        conn.close();

        this.voteMsgId = msgId;
    }

    public boolean readyForVote() {
        return playerCount > 0 && date != null && PHASE_SETUP.equals(phase);
    }

    public int getId() {
        return id;
    }

    public String getSessionString() throws SQLException {
        if(PHASE_SETUP.equals(phase)) {
            String res = "Your session is currently being set up.\n\n";
            if(playerCount <= 0 || date == null || dmComment == null)  {
                res += "I need a few more information:\n";
                if(playerCount <= 0) {
                    res += "**Player Count**: How many players do you want?\n" +
                            "\tUse '!playerCount #' to set the player count.\n";
                }
                if(date == null) {
                    res += "**Date of the session**: When will the session take place?\n" +
                            "\tUse '!sessionDate YYYY-MM-DD' to set the date of play.\n";
                }
                if(dmComment == null) {
                    res += "**Comment** (Optional): Do you want to say anything in the " +
                            "session announcement?\n" +
                            "\tUse '!dmComment TEXT' to specify a message to the players.\n";
                }
                res += "\n";
            }

            if(playerCount > 0 || date != null || dmComment != null) {
                res += "These are the information about the session you already entered:\n";
                if(playerCount > 0) {
                    res += "**Player Count**: Your session will have up to " + playerCount + " players.\n" +
                            "\tTo change this use '!playerCount #'.\n";
                }
                if(date != null) {
                    LocalDate localDate = date.toLocalDate();
                    LocalDate now =LocalDate.now();

                    res += "**Date of the session**: The session will take place at the date "
                            + localDate.getDayOfMonth() + "-" + localDate.getMonthValue() + "-" + localDate.getYear()
                            + ". This is in " + DAYS.between(now, localDate) + " Days.\n" +
                            "\tTo change this use '!sessionDate DD-MM-YYYY'.\n";
                }
                if(dmComment != null) {
                    res += "**Comment**: The following comment will be sent to the players:\n" +
                            "\t\"" + dmComment + "\".\n" +
                            "\tTo change this use '!dmComment TEXT'.\n";
                }
                res += "\n";
            }

            if(playerCount > 0 && date != null) {
                res += "Everything seems to be set up! If you want to start voting use" +
                        " '!startVoting'. After you start the voting process the " +
                        "details you entered cannot be changed without creating a new session!\n\n";
            }

            res += "If you want to cancel the session planning process you can use '!cancelSession'.";
            return res;
        } else if (PHASE_VOTING.equals(phase)) {
            UserTable dm = UserTable.getUserWithId(dmId);
            if(dm == null)
                return "DM with id " + dmId + " does not seem to exist.";

            String res = dm.getUsername() + "#" + dm.getDiscriminator() + " has planned a session! Hurray!\n" +
                    "The session will take place on the " + date.toString() + " with space for " + playerCount + " players.\n" +
                    (dmComment == null ? "" : "Message from DM: \"" + dmComment + "\"\n") +
                    "To join the raffle react to this message with :white_check_mark: .\n" +
                    "Also make sure that you are registered (!register in the bot channel).\n";

            assert(id != -1);
            ArrayList<UserTable> votes = VoteTable.getVotes(id);
            if(!votes.isEmpty()) {
                res += "\nCurrently the following people have entered the raffle:\n";
                for(UserTable user : votes) {
                    res += "**" + user.getUsername() + "**\n";
                }
            }

            return res;
        }
        return "getSessionString when phase is not SETUP or PHASE_VOTING!";
    }

}
