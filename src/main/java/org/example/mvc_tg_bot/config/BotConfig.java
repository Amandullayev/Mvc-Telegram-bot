package org.example.mvc_tg_bot.config;

import org.example.mvc_tg_bot.bot.MyTelegramBot;
import org.example.mvc_tg_bot.service.CartService;
import org.example.mvc_tg_bot.service.OrderService;
import org.example.mvc_tg_bot.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public MyTelegramBot myTelegramBot(
            @Value("${bot.token}") String token,
            @Value("${bot.username}") String username,
            @Value("${bot.admin-id}") Long adminId,
            OrderService orderService,
            CartService cartService,
            ProductService productService+
    ) {
        return new MyTelegramBot(token, username, adminId, orderService, cartService, productService);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(MyTelegramBot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        return botsApi;
    }
}