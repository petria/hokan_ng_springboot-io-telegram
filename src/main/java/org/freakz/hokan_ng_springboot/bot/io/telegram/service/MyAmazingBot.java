package org.freakz.hokan_ng_springboot.bot.io.telegram.service;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;


public class MyAmazingBot extends TelegramLongPollingBot {

    private final TelegramConnectService connectService;

    public MyAmazingBot(TelegramConnectService connectService) {
        this.connectService = connectService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            connectService.sendMessageToEngine(update.getMessage().getText(), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId());
//            sendMessageToEngine(update.getMessage().getText(), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId());

/*            SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                    .setChatId(update.getMessage().getChatId())
                    .setText(update.getMessage().getText());
            try {
                sendMessage(message); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }*/
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
