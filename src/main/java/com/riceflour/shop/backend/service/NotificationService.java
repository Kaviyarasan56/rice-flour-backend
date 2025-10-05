package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final String token = "8447374625:AAGKX5Qa2f_27gpi0_zB2J6KQTvO4OMhyiY";
    private final String chatId = "5525211145";

    private final RestTemplate restTemplate = new RestTemplate();
    private final OrderRepository orderRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public NotificationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void sendOrderNotification(Order order) {
        try {
            String dateLabel = getDateLabelWithActualDate(order.getDate(), order.getCreatedAt());
            String slotLabel = getSlotLabel(order.getSlot());
            String paymentLabel = getPaymentLabel(order);

            StringBuilder sb = new StringBuilder();
            sb.append("🛒 புதிய ஆர்டர் வந்துள்ளது\n\n");
            sb.append("ஆர்டர் எண்: ").append(order.getId()).append("\n");
            
            if (order.getUser() != null) {
                sb.append("பெயர்: ").append(order.getUser().getName() == null ? "பொது" : order.getUser().getName()).append("\n");
                sb.append("ஊர்: ").append(order.getUser().getVillage() == null ? "இல்லை" : order.getUser().getVillage()).append("\n");
                sb.append("தொலைபேசி: ").append(order.getUser().getPhone() == null ? "இல்லை" : order.getUser().getPhone()).append("\n");
            }
            
            sb.append("அளவு: ").append(order.getQuantity()).append("\n");
            sb.append("மொத்த விலை: ₹").append(String.format("%.2f", order.getTotalPrice())).append("\n");
            sb.append("தேதி: ").append(dateLabel).append("\n");
            sb.append("நேரம்: ").append(slotLabel).append("\n");
            sb.append("Payment: ").append(paymentLabel).append("\n");
            sb.append("கருவி ஐடி: ").append(order.getDeviceId()).append("\n");
            sb.append("குறிப்புகள்: ").append(order.getInstructions() == null ? "இல்லை" : order.getInstructions()).append("\n");

            sendTelegramMessage(sb.toString());
        } catch (Exception e) {
            System.err.println("Failed to send Telegram notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send at 12 AM (midnight) for morning slot summary
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void sendMorningSlotSummary() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        sendSlotSummaryForDate(today, "morning", "📦 Today Morning Slot (Final)");
    }

    // Send at 10 AM for evening slot summary
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Kolkata")
    public void sendEveningSlotSummary() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        sendSlotSummaryForDate(today, "evening", "📦 Today Evening Slot (Final)");
    }

    private void sendSlotSummaryForDate(LocalDate targetDate, String slot, String title) {
        try {
            // Calculate which orders match this actual date
            List<Order> orders = orderRepository.findByDateAndSlotAndStatus("today", slot, Order.Status.PENDING)
                .stream()
                .filter(o -> getActualDeliveryDate(o.getDate(), o.getCreatedAt()).equals(targetDate))
                .collect(Collectors.toList());
            
            orders.addAll(orderRepository.findByDateAndSlotAndStatus("tomorrow", slot, Order.Status.PENDING)
                .stream()
                .filter(o -> getActualDeliveryDate(o.getDate(), o.getCreatedAt()).equals(targetDate))
                .collect(Collectors.toList()));
            
            if (orders.isEmpty()) {
                sendTelegramMessage(title + "\n\nNo orders for this slot.");
                return;
            }

            int totalOrders = orders.size();
            int totalQuantity = orders.stream().mapToInt(Order::getQuantity).sum();
            
            Map<String, List<Order>> ordersByUser = orders.stream()
                .collect(Collectors.groupingBy(o -> 
                    o.getUser() != null && o.getUser().getName() != null ? o.getUser().getName() : "Guest"
                ));

            StringBuilder sb = new StringBuilder();
            sb.append(title).append(" - ").append(targetDate.format(dateFormatter)).append("\n\n");
            sb.append("Total Orders: ").append(totalOrders).append("\n");
            sb.append("Total Quantity: ").append(totalQuantity).append("\n\n");

            for (Map.Entry<String, List<Order>> entry : ordersByUser.entrySet()) {
                String userName = entry.getKey();
                List<Order> userOrders = entry.getValue();
                
                int paidQty = userOrders.stream()
                    .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                    .mapToInt(Order::getQuantity)
                    .sum();
                
                int codQty = userOrders.stream()
                    .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.COD_PENDING)
                    .mapToInt(Order::getQuantity)
                    .sum();
                
                int totalUserQty = paidQty + codQty;
                
                sb.append("- ").append(userName).append(": ").append(totalUserQty);
                if (paidQty > 0 && codQty > 0) {
                    sb.append(" (✅ Paid: ").append(paidQty).append(", 💵 COD: ").append(codQty).append(")");
                } else if (paidQty > 0) {
                    sb.append(" (✅ Paid)");
                } else {
                    sb.append(" (💵 COD)");
                }
                sb.append("\n");
            }

            sendTelegramMessage(sb.toString());
        } catch (Exception e) {
            System.err.println("Failed to send slot summary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendTelegramMessage(String message) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.telegram.org/bot" + token + "/sendMessage")
                .queryParam("chat_id", chatId)
                .queryParam("text", message)
                .build()
                .toUriString();

        restTemplate.getForObject(url, String.class);
    }

    private String getDateLabelWithActualDate(String date, java.time.Instant createdAt) {
        LocalDate actualDate = getActualDeliveryDate(date, createdAt);
        String formatted = actualDate.format(dateFormatter);
        
        if ("today".equalsIgnoreCase(date)) {
            return "இன்று (" + formatted + ")";
        } else if ("tomorrow".equalsIgnoreCase(date)) {
            return "நாளை (" + formatted + ")";
        }
        return "தெரியவில்லை (" + formatted + ")";
    }

    private LocalDate getActualDeliveryDate(String dateLabel, java.time.Instant createdAt) {
        LocalDate orderDate = LocalDateTime.ofInstant(createdAt, ZoneId.of("Asia/Kolkata")).toLocalDate();
        if ("today".equalsIgnoreCase(dateLabel)) {
            return orderDate;
        } else if ("tomorrow".equalsIgnoreCase(dateLabel)) {
            return orderDate.plusDays(1);
        }
        return orderDate;
    }

    private String getSlotLabel(String slot) {
        if ("morning".equalsIgnoreCase(slot)) return "காலை";
        if ("evening".equalsIgnoreCase(slot)) return "மாலை";
        return "தெரியவில்லை";
    }

    private String getPaymentLabel(Order order) {
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            return "✅ Success (UPI)";
        } else if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return "💵 Cash on Delivery";
        }
        return "⏳ Pending";
    }
}