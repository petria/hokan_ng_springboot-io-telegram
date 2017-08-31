package org.freakz.hokan_ng_springboot.bot.io.telegram.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcEvent;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.common.jms.api.JmsSender;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Channel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelStartupState;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelState;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelStats;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.IrcLog;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Network;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.User;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.UserChannel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelStatsService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.IrcLogService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.NetworkService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.UserChannelService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.UserService;
import org.freakz.hokan_ng_springboot.bot.common.util.StringStuff;
import org.freakz.hokan_ng_springboot.bot.io.telegram.jms.EngineCommunicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Date;

@Service
@Slf4j
public class TelegramConnectService implements CommandLineRunner {

    private static final String NETWORK_NAME = "telegramNetwork";

    private static final String CHANNEL_NAME = "telegramChannel";

    @Autowired
    private EngineCommunicator engineCommunicator;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private ChannelStatsService channelStatsService;

    @Autowired
    private IrcLogService ircLogService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private UserChannelService userChannelService;

    @Autowired
    private UserService userService;

    @Autowired
    private JmsSender jmsSender;


    private Network getNetwork() {
        Network network = networkService.getNetwork(NETWORK_NAME);
        if (network == null) {
            network = new Network(NETWORK_NAME);
        }
        return networkService.save(network);
    }

    private Channel getChannel(String channelName) {
        Channel channel;
        channel = channelService.findByNetworkAndChannelName(getNetwork(), channelName);

        if (channel == null) {
            channel = channelService.createChannel(getNetwork(), channelName);
        }
        channel.setChannelStartupState(ChannelStartupState.JOIN);
        channel.setChannelState(ChannelState.JOINED);
        return channelService.save(channel);
    }

    private Channel getChannel(IrcEvent ircEvent) {
        return getChannel(ircEvent.getChannel());
    }

    private ChannelStats getChannelStats(Channel channel) {
        ChannelStats channelStats = channelStatsService.findFirstByChannel(channel);
        if (channelStats == null) {
            channelStats = new ChannelStats();
            channelStats.setChannel(channel);
        }
        return channelStats;
    }

    private User getUser(IrcEvent ircEvent) {
        User user;
        User maskUser = this.userService.getUserByMask(ircEvent.getMask());
        if (maskUser != null) {
            user = maskUser;
        } else {
            user = this.userService.findFirstByNick(ircEvent.getSender());
            if (user == null) {
                user = new User(ircEvent.getSender());
                user = userService.save(user);
            }
        }
        user.setRealMask(StringStuff.quoteRegExp(ircEvent.getMask()));
        this.userService.save(user);
        return user;
    }

    public void sendMessageToEngine(String message, String sender, Long chatId) {

        IrcLog ircLog = this.ircLogService.addIrcLog(new Date(), sender, CHANNEL_NAME, message);

        Network nw = getNetwork();
        nw.addToLinesReceived(1);
        this.networkService.save(nw);

        IrcMessageEvent ircEvent = new IrcMessageEvent("" + chatId, NETWORK_NAME, CHANNEL_NAME, sender, "telegramLogin", "telegramHost", message);

        User user = getUser(ircEvent);
        Channel ch = getChannel(ircEvent);
        ChannelStats channelStats = getChannelStats(ch);
        channelStats.addToLinesReceived(1);
        channelStatsService.save(channelStats);

        UserChannel userChannel = userChannelService.getUserChannel(user, ch);
        if (userChannel == null) {
            userChannel = new UserChannel(user, ch);
        }
        userChannel.setLastIrcLogID(ircLog.getId() + "");
        userChannel.setLastMessageTime(new Date());
        userChannelService.save(userChannel);
        if (message.matches("^\\d+: .*")) {
            String[] split = message.split(":");
            int chanId = Integer.valueOf(split[0]);
            ircEvent.setMessage(split[1].trim());
            engineCommunicator.sendToIrcChannel(ircEvent, chanId);
        } else {
            engineCommunicator.sendToEngine(ircEvent, null);
        }
    }

    private TelegramBot telegramBot;

    private void connectTelegram() {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            telegramBot = new TelegramBot(this);
            botsApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run(String... strings) throws Exception {
        connectTelegram();
    }

    public void handleEngineResponse(EngineResponse response) {
        final String botNick = response.getIrcMessageEvent().getBotNick();
        Long chatId = Long.valueOf(botNick);
        log.debug("Engine reply to: {}", botNick);
        SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                .setChatId(chatId)
                .setText(response.getResponseMessage());
        try {
            telegramBot.sendMessage(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleIrcMessage(IrcMessageEvent event) {
        long chatId = Long.valueOf(event.getParameter());
        log.debug("IRC event: {}", chatId);
        String fromIrc = String.format("%s@%s %s", event.getSender(), event.getChannel(), event.getMessage());
        SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                .setChatId(chatId)
                .setText(fromIrc);
        try {
            telegramBot.sendMessage(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
