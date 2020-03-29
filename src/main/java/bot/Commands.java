package bot;

import database.DBHelper;
import database.table.Session;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Optional;

public class Commands {

    public static Mono<Void> commandSetupSession(MessageCreateEvent event) {
        if(!event.getMessage().getAuthor().isPresent())
            return null;

        User usr = event.getMessage().getAuthor().get();
        try {
            if(!DBHelper.userExists(usr.getId().asString())) {
                return event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(
                                "You (" + usr.getUsername() + ") seem to not be registerd.\n" +
                                        "Did you try '!register'"))
                        .then();
            }

            if(!DBHelper.createSession(usr)) {
                return event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(
                                "Something went wrong!\nDo you already have an active session?"
                        )).then();
            }

            return usr.getPrivateChannel()
                    .flatMap(channel -> channel.createMessage(
                            "Session registered successfully!\n" +
                            "Please specify how many players you want to have in your name with '!playerCount #'"
                    ))
                    .then();

        } catch (SQLException e) {
            String msg = "Error while creating new session!\nGot exception " + e.getMessage();
            return event.getMessage().getChannel()
                    .flatMap(channel -> channel.createMessage(msg))
                    .then();
        }

    }

    public static Mono<Void> commandSpecifyPlayerCount(MessageCreateEvent event) {

        User usr = event.getMessage().getAuthor().get();

        try {
            System.out.println("Before session status");
            Session.SessionStatus status = DBHelper.getSessionState(usr);

            System.out.println("Got session status " + status);
            if(status != Session.SessionStatus.START) {
                return event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(
                                "You cannot specify a player count right now!"
                        ))
                        .then();
            }

            String c = event.getMessage().getContent().get()
                    .substring("!playerCount ".length()).trim();

            try {
                int playerCount = Integer.parseInt(c);
                if(playerCount < 1) {
                    return event.getMessage().getChannel()
                            .flatMap(channel -> channel.createMessage(
                                    "The number of players must be greater than 0. >:("
                            ))
                            .then();
                }
                DBHelper.updatePlayerCount(usr, playerCount);

                return event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(
                                "Successfully specified the number of players"
                        ))
                        .then();

            } catch (NumberFormatException e) {
                return event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(
                                "'" + c + "' is not a number!"
                        ))
                        .then();
            }

        } catch (SQLException e) {
            return event.getMessage().getChannel()
                    .flatMap(channel -> channel.createMessage(
                            "SQL error while trying to specify player count. Got error message\n"
                            + e.getMessage()
                    ))
                    .then();
        }
    }


    public static Mono<Void> commandRegister(MessageCreateEvent event) {
        Optional<User> userOp = event.getMessage().getAuthor();
        assert(userOp.isPresent());
        User user = userOp.get();

        String msg;
        try {
            boolean alreadyExists = DBHelper.addUser(user);
            if(alreadyExists)
                msg = "User " + user.getUsername() + "#" + user.getDiscriminator() + " already registered!";
            else
                msg = "User " + user.getUsername() + "#" + user.getDiscriminator() + " added successfully!";
        } catch (SQLException e) {
            msg = "Error adding user. Got exception " + e.getMessage();
        }

        final String m = msg; //FIXME: Get rid of this.
        return event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(m))
                .then();
    }
}
