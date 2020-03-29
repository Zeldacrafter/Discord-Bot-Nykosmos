package bot;

import database.DBHelper;
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
                            "Please specify how many players you want to have in your name with '!players #'"
                    ))
                    .then();

        } catch (SQLException e) {
            String msg = "Error while creating new session!\nGot exception " + e.getMessage();
            return event.getMessage().getChannel()
                    .flatMap(channel -> channel.createMessage(msg))
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
