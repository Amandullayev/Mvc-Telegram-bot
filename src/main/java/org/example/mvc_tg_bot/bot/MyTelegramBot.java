package org.example.mvc_tg_bot.bot;

import org.example.mvc_tg_bot.model.*;
import org.example.mvc_tg_bot.service.CartService;
import org.example.mvc_tg_bot.service.OrderService;
import org.example.mvc_tg_bot.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

// @Component O‘CHIRILDI! Chunki BotConfig da yaratiladi
public class MyTelegramBot extends TelegramLongPollingBot {

    private final String token;
    private final String username;
    private final Long adminId;

    private final OrderService orderService;
    private final CartService cartService;
    private final ProductService productService;

    // BITTA KATTA KONSTRUKTOR — HAMMA NARSANI BU YERGA OLAMIZ
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
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

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
                case "SETTINGS" -> showSettings(chatId);
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

    // === ADD TO CART ===
    private void addToCart(Long chatId, Long productId) {
        cartService.addToCart(chatId, productId);
        Product p = productService.findById(productId);
        if (p != null) {
            sendMessage(chatId, "*%s* savatchaga qo‘shildi!".formatted(p.getName()));
        }
    }

    // === WELCOME ===
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

    // === MAIN MENU ===
    private InlineKeyboardMarkup getMainMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createButton("Menyu", "MENU")));
        rows.add(List.of(
                createButton("Savatcha", "CART"),
                createButton("Sozlamalar", "SETTINGS")
        ));

        markup.setKeyboard(rows);
        return markup;
    }

    // === SHOW MENU ===
    private void showMenu(Long chatId) {
        List<Product> products = productService.getActiveProducts();
        if (products.isEmpty()) {
            sendMessage(chatId, "Hozircha mahsulot yo‘q.", getBackToMainKeyboard());
            return;
        }

        for (Product p : products) {
            String text = """
                    *%s*
                    %s
                    *%.0f UZS*
                    """.formatted(p.getName(),
                    p.getDescription() != null ? p.getDescription() : "",
                    p.getPrice());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(List.of(List.of(createButton("Qo‘shish", "ADD_" + p.getId()))));
            sendMessage(chatId, text, markup);
        }
        sendMessage(chatId, "Orqaga", getBackToMainKeyboard());
    }

    // === SHOW CART ===
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

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("Buyurtma berish", "ORDER")),
                List.of(createButton("Tozalash", "CLEAR_CART")),
                List.of(createButton("Orqaga", "MAIN_MENU"))
        ));

        sendMessage(chatId, sb.toString(), markup);
    }

    // === SETTINGS ===
    private void showSettings(Long chatId) {
        sendMessage(chatId, "*Sozlamalar*\n• Til: O‘zbek\n• Valyuta: UZS", getBackToMainKeyboard());
    }

    // === ORDER ===
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

    // === CLEAR CART ===
    private void clearCart(Long chatId) {
        cartService.clearCart(chatId);
        sendMessage(chatId, "Savatcha tozalandi!", getBackToMainKeyboard());
    }

    // === NOTIFY ADMIN ===
    private void notifyAdminAboutNewOrder(Order order) {
        if (adminId == null) return;

        StringBuilder sb = new StringBuilder("*Yangi buyurtma!*\n\n");
        sb.append("Raqami: #").append(order.getId()).append("\n");
        sb.append("Foydalanuvchi: ").append(order.getUserId()).append("\n");
        sb.append("Vaqt: ").append(order.getCreatedAt().toString().substring(11, 16)).append("\n\n");

        for (OrderItem item : order.getItems()) {
            sb.append(String.format("• %s × %d = %.0f UZS\n",
                    item.getProductName(), item.getQuantity(),
                    item.getPrice() * item.getQuantity()));
        }
        sb.append("\n*Jami: %.0f UZS*".formatted(order.getTotalPrice()));

        sendMessage(adminId, sb.toString());
    }

    // === BACK BUTTON ===
    private InlineKeyboardMarkup getBackToMainKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(createButton("Orqaga", "MAIN_MENU"))));
        return markup;
    }

    // === BUTTON HELPER ===
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    // === SEND MESSAGE ===
    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
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