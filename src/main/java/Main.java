import bot.Command;
import bot.Commands;
import database.DBHelper;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Map<String, Command> commands = new HashMap<>();


    private static void initCommands() {
        commands.put("register", Commands::commandRegister);
        commands.put("setupSession", Commands::commandSetupSession);
    }


    public static void main(String[] args) {
        DBHelper.createTables();

        DiscordClientBuilder builder = new DiscordClientBuilder(PrivateData.BOT_TOKEN);
        DiscordClient client = builder.build();

        client.getEventDispatcher().on(ReadyEvent.class)
            .subscribe(event -> {
                User self = event.getSelf();
                System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
            });

        client.getEventDispatcher().on(ReactionAddEvent.class)
            .subscribe(Main::evalAddReaction);

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

        initCommands();

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

    private static void evalAddReaction(ReactionAddEvent event) {
        User user = event.getUser().block();
        Message m = event.getMessage().block();

        if(user == null || m == null || !m.getContent().isPresent())
            return;

        String currContent = m.getContent().get();

        boolean foundUser = false;
        int cnt = 0;

        String userPatterns = "\\*\\*(.*)\\*\\*";
        Matcher matcher = Pattern.compile(userPatterns).matcher(currContent);
        while (matcher.find()) {
            cnt++;
            if(matcher.group(1).equals(user.getUsername())) {
                System.out.println("Already found user " + user.getUsername());
                foundUser = true;
                break;
            }
        }

        if(cnt == 0)
            currContent += "\nUsers:";
        if(!foundUser)
            currContent+= "\n**" + user.getUsername() + "**";

        final String res = currContent;
        m.edit(messageEditSpec -> messageEditSpec.setContent(res))
                .subscribe();
    }
}
