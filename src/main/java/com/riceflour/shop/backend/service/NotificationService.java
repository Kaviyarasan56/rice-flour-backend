package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NotificationService {

    private final String token = "8447374625:AAGKX5Qa2f_27gpi0_zB2J6KQTvO4OMhyiY";
    private final String chatId = "5525211145";

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

            // Use UriComponentsBuilder to encode properly
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.telegram.org/bot" + token + "/sendMessage")
                    .queryParam("chat_id", chatId)
                    .queryParam("text", message)
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            System.out.println("Telegram API response for order ID " + order.getId() + ": " + response);

        } catch (Exception e) {
            System.err.println("Failed to send Telegram notification for order ID " + order.getId());
            e.printStackTrace();
        }
    }
}
