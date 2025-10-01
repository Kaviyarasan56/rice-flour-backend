package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NotificationService {

    // ⚠️ Better to move these to ENV vars in production
    private final String token = "8447374625:AAGKX5Qa2f_27gpi0_zB2J6KQTvO4OMhyiY";
    private final String chatId = "5525211145";

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOrderNotification(Order order) {
        try {
            // -------------------------
            // --- MESSAGE CONTENT (தமிழில்) -----
            String dateLabel = "தெரியவில்லை";
            if ("today".equalsIgnoreCase(order.getDate())) dateLabel = "இன்று";
            else if ("tomorrow".equalsIgnoreCase(order.getDate())) dateLabel = "நாளை";

            String slotLabel = "தெரியவில்லை";
            if ("morning".equalsIgnoreCase(order.getSlot())) slotLabel = "காலை";
            else if ("evening".equalsIgnoreCase(order.getSlot())) slotLabel = "மாலை";

            StringBuilder sb = new StringBuilder();
            sb.append("🛒 புதிய ஆர்டர் வந்துள்ளது\n");
            sb.append("ஆர்டர் எண்: ").append(order.getId()).append("\n");
            sb.append("அளவு: ").append(order.getQuantity()).append("\n");
            sb.append("மொத்த விலை: ₹").append(order.getTotalPrice()).append("\n");
            sb.append("தேதி: ").append(dateLabel).append("\n");
            sb.append("நேரம்: ").append(slotLabel).append("\n");
            sb.append("கருவி ஐடி: ").append(order.getDeviceId()).append("\n");
            sb.append("குறிப்புகள்: ").append(order.getInstructions() == null ? "இல்லை" : order.getInstructions()).append("\n");

            // If a user is linked, include user info
            if (order.getUser() != null) {
                sb.append("பெயர்: ").append(order.getUser().getName() == null ? "பொது" : order.getUser().getName()).append("\n");
                sb.append("ஊர்: ").append(order.getUser().getVillage() == null ? "இல்லை" : order.getUser().getVillage()).append("\n");
                sb.append("தொலைபேசி: ").append(order.getUser().getPhone() == null ? "இல்லை" : order.getUser().getPhone()).append("\n");
            } else {
                sb.append("பெயர்: பொது\nஊர்: இல்லை\nதொலைபேசி: இல்லை\n");
            }

            String message = sb.toString();
            // -------------------------

            // Build URL
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
