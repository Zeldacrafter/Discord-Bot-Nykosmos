package bot;

import Main.Main;
import Main.PrivateData;
import com.wl.ReservoirLottery;
import database.table.SessionTable;
import database.table.UserTable;
import database.table.VoteTable;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
        try {
            //FIXME: Gets not only active votes but also accepted/declined ones.
            // This should never happen but we might want to add a check anyways.
            ArrayList<VoteTable> participants = VoteTable.getVotes(session.getId());

            int[] raffleCount = new int[participants.size()];
            int totalCount = 0;
            for(int i = 0; i < participants.size(); ++i) {
                raffleCount[i] = 100;
                totalCount += raffleCount[i];
            }
            double[] prob = new double[participants.size()];
            for(int i = 0; i < prob.length; ++i)
                prob[i] = (double)raffleCount[i]/totalCount;

            ReservoirLottery lottery = new ReservoirLottery(prob, participants.size(), ThreadLocalRandom::current);

            UserTable dm = UserTable.getUserWithId(session.getDmId());
            assert(dm != null);

            String resString =
                    "Voting for the session by " + dm.getUsername() + "#" + dm.getDiscriminator() + " has ended!\n" +
                    "The following players were chosen:\n";
            for(int i = 0; i < session.getPlayerCount(); ++i) {
                VoteTable winner = participants.get(lottery.draw());
                UserTable userWinner = UserTable.getUserWithId(winner.getPlayerId());
                assert(userWinner != null);
                resString += "**" + userWinner.getUsername() + "#" + userWinner.getDiscriminator() + "**\n";
                winner.setStatus(VoteTable.STATUS_ACCEPTED);
            }
            for(VoteTable p : participants)
                if (VoteTable.STATUS_UNDECIDED.equals(p.getStatus()))
                    p.setStatus(VoteTable.STATUS_DECLINED);

            session.setPhase(SessionTable.PHASE_DONE);

            Mono<MessageChannel> c = Main.getClient().getChannelById(Snowflake.of(PrivateData.BOT_CHANNEL)).cast(MessageChannel.class);

            String res = resString;
            c.flatMap(channel -> channel.createMessage(res)).subscribe();
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
