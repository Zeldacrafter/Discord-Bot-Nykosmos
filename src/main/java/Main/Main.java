package Main;

import bot.Command;
import bot.Commands;
import bot.VotingCloseTimer;
import database.DBHelper;
import database.table.VoteTable;
import database.table.SessionTable;
import database.table.UserTable;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Map<String, Command> commands = new HashMap<>();

    private static DiscordClient client;

    private static void initCommands() {
        commands.put("register", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() != Channel.Type.DM)
                .flatMap(a -> Commands.commandRegister(event)));
        commands.put("setupSession", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() != Channel.Type.DM)
                .flatMap(a -> Commands.commandSetupSession(event)));
        commands.put("playerCount", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() == Channel.Type.DM)
                .flatMap(a -> Commands.commandPlayerCount(event)));
        commands.put("cancelSession", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() == Channel.Type.DM)
                .flatMap(a -> Commands.commandCancelSession(event)));
        commands.put("dmComment", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() == Channel.Type.DM)
                .flatMap(a -> Commands.commandDmComment(event)));
        commands.put("sessionDate", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() == Channel.Type.DM)
                .flatMap(a -> Commands.commandSessionDate(event)));
        commands.put("voteEnd", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() == Channel.Type.DM)
                .flatMap(a -> Commands.commandVoteEnd(event)));
        commands.put("startVoting", event -> event.getMessage().getChannel()
                .filter(msgChannel -> msgChannel.getType() == Channel.Type.DM)
                .flatMap(a -> Commands.commandStartVoting(event)));

    }


    public static void main(String[] args) {
        DBHelper.createTables();
        initCommands();
        VotingCloseTimer.startVotingCloseTimers();
        startClient();
    }

    private static void startClient() {
        DiscordClientBuilder builder = new DiscordClientBuilder(PrivateData.BOT_TOKEN);
        client = builder.build();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });

        client.getEventDispatcher().on(ReactionAddEvent.class)
                .subscribe(Main::evalAddReaction);

        client.getEventDispatcher().on(ReactionRemoveEvent.class)
                .subscribe(Main::evalRemoveReaction);

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(event -> channelValid(event.getMessage().getChannel()))
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith('!' + entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()
                        )
                )
                .subscribe();

        client.login().block();
    }

    private static boolean channelValid(Mono<MessageChannel> channel) {
        Channel c = channel.block();
        if(c == null)
            return false;
        if(c.getType() == Channel.Type.DM)
            return true; //Private channels are always okay

        // FIXME: We might need a better solution long term.
        //        Hardcoding this is temporary.
        return PrivateData.BOT_CHANNEL.equals(c.getId().asString());
    }

    private static void evalRemoveReaction(ReactionRemoveEvent event) {
        if(!"✅".equals(event.getEmoji().asUnicodeEmoji().get().getRaw())) //FIXME: Ugh, thats ugly
            return;

        User user = event.getUser().block();
        Message m = event.getMessage().block();
        if(user == null || m == null || !m.getContent().isPresent())
            return;

        try {
            if(!UserTable.userWithIdExists(user.getId().asString()))
                return;

            SessionTable session = SessionTable.getFromVoteMsgId(m.getId().asString());

            if(session == null)
                return; // Current message is not active for voting.

            VoteTable.delete(session.getId(), user.getId().asString());

            String newMsg = session.getSessionString();
            m.edit(messageEditSpec -> messageEditSpec.setContent(newMsg))
                    .subscribe();
        } catch (SQLException e) {
            System.out.println("SQL error when handling reaction event!");
        }
    }

    private static void evalAddReaction(ReactionAddEvent event) {

        if(!"✅".equals(event.getEmoji().asUnicodeEmoji().get().getRaw())) //FIXME: Ugh, thats ugly
            return;

        User user = event.getUser().block();
        Message m = event.getMessage().block();
        if(user == null || m == null || !m.getContent().isPresent())
            return;

        try {
            if(!UserTable.userWithIdExists(user.getId().asString()))
                return;

            SessionTable session = SessionTable.getFromVoteMsgId(m.getId().asString());

            if(session == null)
                return; // Current message is not active for voting.

            VoteTable.insert(session.getId(), user.getId().asString());

            String newMsg = session.getSessionString();
            m.edit(messageEditSpec -> messageEditSpec.setContent(newMsg))
                    .subscribe();

        } catch (SQLException e) {
            System.out.println("SQL error when handling reaction event!");
        }

    }

    public static DiscordClient getClient() {
        return client;
    }
}
