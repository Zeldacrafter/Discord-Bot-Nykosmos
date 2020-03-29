import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Map<String, Command> commands = new HashMap<>();
    private static final ArrayList<String> channels = new ArrayList<>(); // TODO

    private static void initCommands() {
        commands.put("ping", event -> event.getMessage().getChannel()
                     .flatMap(channel -> channel.createMessage("Pong!"))
                     .then());


    }

    public static void main(String[] args) {

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

    static void evalAddReaction(ReactionAddEvent event) {
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
