package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NotificationService {

    private final String token = "YOUR_TELEGRAM_BOT_TOKEN";
    private final String chatId = "YOUR_CHAT_ID";

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOrderNotification(Order order) {
        try {
            String message = "உங்கள் ஆர்டர் வெற்றிகரமாக பதிவுசெய்யப்பட்டது!\n" +
                    "ஆர்டர் எண்: " + order.getId() + "\n" +
                    "அளவு: " + order.getQuantity() + "\n" +
                    "தேதி: " + (order.getDate().equals("today") ? "இன்று" : "நாளை") + "\n" +
                    "நேரம்: " + (order.getSlot().equals("morning") ? "காலை" : "மாலை") + "\n" +
                    "குறிப்பு: " + (order.getInstructions() == null ? "இல்லை" : order.getInstructions()) +
                    "\nமுகவரி: " + (order.getAddress() == null ? "இல்லை" : order.getAddress());

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.telegram.org/bot" + token + "/sendMessage")
                    .queryParam("chat_id", chatId)
                    .queryParam("text", message)
                    .build()
                    .toUriString();

            restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            System.err.println("Failed to send Telegram notification for order ID " + order.getId());
            e.printStackTrace();
        }
    }
}
