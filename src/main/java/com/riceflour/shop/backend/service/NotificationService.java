package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class NotificationService {

    private final String token = "8447374625:AAGKX5Qa2f_27gpi0_zB2J6KQTvO4OMhyiY";
    private final String chatId = "5525211145";

    public void sendOrderNotification(Order order) {
        try {
            String message = "🍚 New Rice Flour Order!\n" +
                    "Customer: " + order.getCustomerName() + "\n" +
                    "Phone: " + order.getPhone() + "\n" +
                    "Email: " + order.getEmail() + "\n" +
                    "Quantity: " + order.getQuantity() + "\n" +
                    "Instructions: " + (order.getInstructions() == null ? "None" : order.getInstructions());

            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            String url = "https://api.telegram.org/bot" + token +
                    "/sendMessage?chat_id=" + chatId + "&text=" + encodedMessage;

            new RestTemplate().getForObject(url, String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
