package bot;

import database.table.SessionTable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class VotingCloseTimer extends TimerTask {

    private SessionTable session;

    public VotingCloseTimer(SessionTable session) {
        Timer timer = new Timer();
        LocalDateTime endDate = session.getVoteEndDateTime();
        Date date = Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant());
        timer.schedule(this, date);

        this.session = session;
    }

    @Override
    public void run() {
        System.out.println("Voting closed!");
    }

    public static void startVotingCloseTimers() {
        try {
            ArrayList<SessionTable> votingSessions = SessionTable.getSessionsByPhase(SessionTable.PHASE_VOTING);
            for(SessionTable s : votingSessions)
                new VotingCloseTimer(s);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
