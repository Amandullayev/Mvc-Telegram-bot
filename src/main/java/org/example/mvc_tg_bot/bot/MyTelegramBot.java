package org.example.mvc_tg_bot.bot;

import org.example.mvc_tg_bot.model.*;
import org.example.mvc_tg_bot.service.CartService;
import org.example.mvc_tg_bot.service.OrderService;
import org.example.mvc_tg_bot.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class MyTelegramBot extends TelegramLongPollingBot {

    private final String token;
    private final String username;
    private final Long adminId;

    private final OrderService orderService;
    private final CartService cartService;
    private final ProductService productService;

    public MyTelegramBot(
            @Value("${bot.token}") String token,
            @Value("${bot.username}") String username,
            @Value("${bot.admin-id}") Long adminId,
            OrderService orderService,
            CartService cartService,
            ProductService productService
    ) {
        this.token = token;
        this.username = username;
        this.adminId = adminId;
        this.orderService = orderService;
        this.cartService = cartService;
        this.productService = productService;
    }

    @Override
    public String getBotUsername() { return username; }
    @Override
    public String getBotToken() { return token; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                sendWelcomeMessage(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (data) {
                case "MENU" -> showMenu(chatId);
                case "CART" -> showCart(chatId);
                case "MAIN_MENU" -> sendWelcomeMessage(chatId);
                case "ORDER" -> processOrder(chatId);
                case "CLEAR_CART" -> clearCart(chatId);
                default -> {
                    if (data.startsWith("ADD_")) {
                        Long productId = Long.parseLong(data.split("_")[1]);
                        addToCart(chatId, productId);
                    }
                }
            }
        }
    }

    private void addToCart(Long chatId, Long productId) {
        cartService.addToCart(chatId, productId);
        Product p = productService.findById(productId);
        if (p != null) {
            sendMessage(chatId, "*%s* savatchaga qo‘shildi!".formatted(p.getName()));
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcome = """
                Assalomu alaykum! 
                *MyMVC Bot* ga xush kelibsiz!
                
                Bu yerda siz:
                • Taomlarni ko‘rishingiz
                • Savatchaga qo‘shishingiz
                • Buyurtma berishingiz mumkin
                """;
        sendMessage(chatId, welcome, getMainMenuKeyboard());
    }

    private InlineKeyboardMarkup getMainMenuKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(createButton("Menyu", "MENU")))
                .keyboardRow(List.of(
                        createButton("Savatcha", "CART")
                ))
                .build();
    }

    private void showMenu(Long chatId) {
        List<Product> products = productService.getActiveProducts();
        if (products.isEmpty()) {
            sendMessage(chatId, "<b>Menyu bo‘sh</b>", getBackToMainKeyboard());
            return;
        }

        for (Product p : products) {
            String caption = """
                <b>%s</b>
                %s
                
                <b>%.0f UZS</b>
                """.formatted(
                    p.getName(),
                    p.getDescription() != null ? p.getDescription() : "Tavsif yo‘q",
                    p.getPrice()
            );

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(createButton("Qo‘shish", "ADD_" + p.getId())))
                    .build();

            if (p.getImgUrl() != null && !p.getImgUrl().isEmpty()) {
                sendPhoto(chatId, p.getImgUrl(), caption, keyboard);
            } else {
                sendMessage(chatId, caption, keyboard);
            }
        }
        sendMessage(chatId, "Orqaga", getBackToMainKeyboard());
    }
    private void sendPhoto(Long chatId, String photoUrl, String caption, InlineKeyboardMarkup keyboard) {
        SendPhoto photo = SendPhoto.builder()
                .chatId(chatId.toString())
                .photo(new InputFile(photoUrl))
                .caption(caption)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build();
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            sendMessage(chatId, caption, keyboard); // Agar rasm bo‘lmasa
        }
    }

    private void showCart(Long chatId) {
        List<Cart> cartItems = cartService.getCart(chatId);
        if (cartItems.isEmpty()) {
            sendMessage(chatId, "*Savatcha bo‘sh*", getBackToMainKeyboard());
            return;
        }

        StringBuilder sb = new StringBuilder("Savatchangiz:\n\n");
        for (Cart item : cartItems) {
            sb.append(String.format("• *%s* × %d = %.0f UZS\n",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getProduct().getPrice() * item.getQuantity()));
        }
        sb.append("\n*Jami: %.0f UZS*".formatted(cartService.getTotalPrice(chatId)));

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(createButton("Buyurtma berish", "ORDER")))
                .keyboardRow(List.of(createButton("Tozalash", "CLEAR_CART")))
                .keyboardRow(List.of(createButton("Orqaga", "MAIN_MENU")))
                .build();

        sendMessage(chatId, sb.toString(), markup);
    }

    private void processOrder(Long chatId) {
        Order order = orderService.createOrder(chatId);
        if (order == null) {
            sendMessage(chatId, "Savatcha bo‘sh!");
            return;
        }

        String text = """
                *Buyurtmangiz qabul qilindi!*
                Raqami: #%d
                Jami: %.0f UZS
                """.formatted(order.getId(), order.getTotalPrice());

        sendMessage(chatId, text, getBackToMainKeyboard());
        notifyAdminAboutNewOrder(order);
    }

    private void clearCart(Long chatId) {
        cartService.clearCart(chatId);
        sendMessage(chatId, "Savatcha tozalandi!", getBackToMainKeyboard());
    }

    private void notifyAdminAboutNewOrder(Order order) {
        if (adminId == null) return;

        StringBuilder sb = new StringBuilder("*Yangi buyurtma!*\n\n");
        sb.append("Raqami: #").append(order.getId()).append("\n");
        sb.append("Foydalanuvchi: ").append(order.getUserId()).append("\n");
        sb.append("Vaqt: ").append(order.getCreatedAt().toString().substring(11, 16)).append("\n\n");

        for (OrderItem item : order.getItems()) {
            sb.append(String.format("• %s × %d = %.0f U Pic\n",
                    item.getProductName(), item.getQuantity(),
                    item.getPrice() * item.getQuantity()));
        }
        sb.append("\n*Jami: %.0f UZS*".formatted(order.getTotalPrice()));

        sendMessage(adminId, sb.toString());
    }

    private InlineKeyboardMarkup getBackToMainKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(createButton("Orqaga", "MAIN_MENU")))
                .build();
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build();
        executeSafely(message);
    }
    private void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    private void executeSafely(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}