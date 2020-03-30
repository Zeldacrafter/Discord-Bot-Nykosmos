package bot;

import Main.Main;
import Main.PrivateData;

import database.table.SessionTable;
import database.table.UserTable;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Optional;

public class Commands {

    public static Mono<Void> commandSetupSession(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();

        if(!event.getMessage().getAuthor().isPresent())
            return makeMsg(channel, "Could not find author in method 'commandSetupSession'");

        User user = event.getMessage().getAuthor().get();
        try {
            if(!UserTable.userWithIdExists(user.getId().asString()))
                return makeMsg(channel,
                        "Cannot find user '" + user.getUsername() + "#" + user.getDiscriminator() + "'.\n" +
                                        "Are you registered via '!register'?");

            SessionTable insertedSession = SessionTable.createSession(user.getId().asString());
            if(insertedSession == null)
                return makeMsg(channel,
                        "Cannot create session. Do you already have an active session?");

            return makePrivMsg(user.getPrivateChannel(),
                    "Session registered successfully!\n" +
                             insertedSession.getSessionString());

        } catch (SQLException e) {
            return makeMsg(channel,
                    "SQL error while creating new session!\nGot exception " + e.getMessage());
        }

    }

    public static Mono<Void> commandPlayerCount(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();

        if(!event.getMessage().getAuthor().isPresent())
            return makeMsg(channel, "Could not find author in method 'commandSpecifyPlayerCount'");

        User user = event.getMessage().getAuthor().get();

        try {
            SessionTable currSession = SessionTable.getActiveSession(user.getId().asString());

            if(currSession == null)
                return makeMsg(channel,
                    "Could not find session. Did you already register a" +
                            "new session with '!setupSession'?");

            if(!SessionTable.PHASE_SETUP.equals(currSession.getPhase()))
                return makeMsg(channel,
                        "Your session is not currently in planning phase.\n" +
                        "You cannot specify a player count now.");

            if(!event.getMessage().getContent().isPresent())
                return makeMsg(channel,
                        "Could not find message content in'commandSpecifyPlayerCount'");

            String c = event.getMessage().getContent().get()
                    .substring("!playerCount".length()).trim();
            try {
                int playerCount = Integer.parseInt(c);
                if(playerCount < 1)
                    return makeMsg(channel,
                            "The number of players must be greater than 0. >:(");

                currSession.setPlayerCount(playerCount);

                return makeMsg(channel, currSession.getSessionString());
            } catch (NumberFormatException e) {
                return makeMsg(channel, "Could not read number '" + c + "'!");
            }

        } catch (SQLException e) {
            return makeMsg(channel,
                            "SQL error while trying to specify player count. Got error message\n"
                            + e.getMessage());
        }
    }

    public static Mono<Void> commandCancelSession(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();

        if (!event.getMessage().getAuthor().isPresent())
            return makeMsg(channel, "Could not find author in method 'commandCancelSession'");

        User user = event.getMessage().getAuthor().get();
        try {
            SessionTable activeSession = SessionTable.getActiveSession(user.getId().asString());
            if (activeSession == null)
                return makeMsg(channel, "You do not seem to have an active session!");

            if (activeSession.deleteActiveSession())
                return makeMsg(channel, "Session deleted successfully!");
            else
                return makeMsg(channel, "Session could not be deleted. This shouldn't ever happen.");

        }catch (SQLException e) {
            return makeMsg(channel,
                    "SQL error while trying to specify cancel session. Got error message\n"
                            + e.getMessage());
        }
    }

    public static Mono<Void> commandDmComment(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();

        if (!event.getMessage().getAuthor().isPresent())
            return makeMsg(channel, "Could not find author in method 'commandDmComment'");

        User user = event.getMessage().getAuthor().get();

        try {
            SessionTable activeSession = SessionTable.getActiveSession(user.getId().asString());
            if (activeSession == null)
                return makeMsg(channel, "You do not seem to have an active session!");

            if(!event.getMessage().getContent().isPresent())
                return makeMsg(channel, "Couldnt find message content.");

            String comment = event.getMessage().getContent().get().substring("!dmComment".length()).trim();
            activeSession.setDmComment(comment);

            return makeMsg(channel, activeSession.getSessionString());
        }catch (SQLException e) {
            return makeMsg(channel,
                    "SQL error while trying to set DM comment. Got error message\n"
                            + e.getMessage());
        }
    }

    public static Mono<Void> commandSessionDate(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();

        if (!event.getMessage().getAuthor().isPresent())
            return makeMsg(channel, "Could not find author in method 'commandSessionDate'");

        User user = event.getMessage().getAuthor().get();

        try {
            SessionTable activeSession = SessionTable.getActiveSession(user.getId().asString());
            if (activeSession == null)
                return makeMsg(channel, "You do not seem to have an active session!");

            if(!event.getMessage().getContent().isPresent())
                return makeMsg(channel, "Couldn't find message content.");

            String dateString = event.getMessage().getContent().get().substring("!sessionDate".length()).trim();
            try {
                Date currDate = Date.valueOf(dateString);
                activeSession.setDate(currDate);
                return makeMsg(channel, activeSession.getSessionString());
            } catch (IllegalArgumentException a) {
                return makeMsg(channel, "Date couldnt be read! Is your format 'YYYY-MM-DD'?");
            }

        }catch (SQLException e) {
            return makeMsg(channel,
                    "SQL error while trying to set date. Got error message\n"
                            + e.getMessage());
        }
    }

    public static Mono<? extends Void> commandStartVoting(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();

        if (!event.getMessage().getAuthor().isPresent())
            return makeMsg(channel, "Could not find author in method 'commandStartVoting'");

        User user = event.getMessage().getAuthor().get();

        try {
            SessionTable activeSession = SessionTable.getActiveSession(user.getId().asString());
            if (activeSession == null)
                return makeMsg(channel, "You do not seem to have an active session!");

            if (!activeSession.readyForVote())
                return makeMsg(channel, "You did not enter the needed information yet!");

            Mono<MessageChannel> c = Main.getClient().getChannelById(Snowflake.of(PrivateData.BOT_CHANNEL)).cast(MessageChannel.class);

            activeSession.setPhase(SessionTable.PHASE_VOTING);

            String sessionString = activeSession.getSessionString();

            return c.flatMap(c1 -> c1.createMessage(sessionString)).flatMap(
                    message -> {
                        try {
                            activeSession.setVoteMsgId(message.getId().asString());
                        } catch (SQLException e) {
                            System.err.println("Could not save vote message id.");
                        }
                        return Mono.just(message);
                    }).then();

        }catch (SQLException e) {
            return makeMsg(channel,
                    "SQL error while trying to start voting. Got error message\n"
                            + e.getMessage());
        }
    }

    public static Mono<Void> commandRegister(MessageCreateEvent event) {
        Mono<MessageChannel> channel = event.getMessage().getChannel();
        Optional<User> userOp = event.getMessage().getAuthor();
        if(!userOp.isPresent())
            return makeMsg(channel, "Could not find message author.");

        User user = userOp.get();
        try {
            UserTable insertedUser = UserTable.insert(user);
            if(insertedUser == null)
                return makeMsg(channel,
                        "User " + user.getUsername() + "#" + user.getDiscriminator() + " already registered!");
            else
                return makeMsg(channel,
                        "User " + user.getUsername() + "#" + user.getDiscriminator() + " added successfully!");
        } catch (SQLException e) {
            return makeMsg(channel, "SQL error adding user. Got exception " + e.getMessage());
        }
    }

    // FIXME: Merge with makeMsg
    private static Mono<Void> makePrivMsg(Mono<PrivateChannel> channel, String message) {
        return channel.flatMap(c -> c.createMessage(message)).then();
    }
    private static Mono<Void> makeMsg(Mono<MessageChannel> channel, String message) {
        return channel.flatMap(c -> c.createMessage(message)).then();
    }

}
