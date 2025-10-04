package com.riceflour.shop.backend.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

  
    // Hardcoded Razorpay credentials
    private final String razorpayKeyId = "rzp_test_RPSteL8rpgHc4n";
    private final String razorpayKeySecret = "jmhKejQ5pnm1HCVNXV5Yqu9u";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Map<String, Object> createRazorpayOrder(String deviceId, Double amount, String date, String slot) throws Exception {
        // Convert to paise (Razorpay uses smallest currency unit)
        int amountInPaise = (int) (amount * 100);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "rcpt_" + UUID.randomUUID().toString().substring(0, 8));

        JSONObject notes = new JSONObject();
        notes.put("deviceId", deviceId);
        notes.put("date", date);
        notes.put("slot", slot);
        orderRequest.put("notes", notes);

        String auth = razorpayKeyId + ":" + razorpayKeySecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Authorization", "Basic " + encodedAuth)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(orderRequest.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Razorpay API error: " + response.body());
        }

        JSONObject responseJson = new JSONObject(response.body());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", responseJson.getString("id"));
        result.put("amount", amountInPaise);
        result.put("keyId", razorpayKeyId);
        result.put("currency", "INR");

        return result;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = bytesToHex(hash);
            
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}