package org.freakz.hokan_ng_springboot.bot.io.telegram.service;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;


public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramConnectService connectService;

    public TelegramBot(TelegramConnectService connectService) {
        this.connectService = connectService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String from = update.getMessage().getFrom().getFirstName() + update.getMessage().getFrom().getLastName();
            connectService.sendMessageToEngine(update.getMessage().getText(), from, update.getMessage().getChatId());
        }
    }

    @Override
    public String getBotUsername() {
        return "HokanThebot";
    }

    @Override
    public String getBotToken() {
        return "412991638:AAGHKCGzfpGrKTLGOSdy81oaknxE-Qw2a20";
    }

}
