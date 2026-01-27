package com.tejas.metlife.claimprocessor.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class DocumentAIService {

    private final OpenAIClient openAIClient;
    private final String deploymentName;

    public DocumentAIService(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.key}") String key,
            @Value("${azure.openai.deployment}") String deployment
    ) {
        this.openAIClient = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();
        this.deploymentName = deployment;
    }

    /**
     * Extract text from image/PDF using GPT-4o Vision
     */
    public String extractTextFromImage(MultipartFile file) {
        try {
            System.out.println("\n[DocumentAIService] ========== STARTING GPT-4o VISION OCR ==========");
            System.out.println("[DocumentAIService] File: " + file.getOriginalFilename());
            System.out.println("[DocumentAIService] Size: " + file.getSize() + " bytes");
            System.out.println("[DocumentAIService] Content Type: " + file.getContentType());
            
            // Convert image to base64
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = file.getContentType();
            
            // Prepare messages for GPT-4o vision
            List<ChatRequestMessage> messages = new ArrayList<>();
            
            // System message
            messages.add(new ChatRequestSystemMessage(
                "You are an expert OCR system. Extract ALL text from the document image exactly as it appears. " +
                "Preserve formatting, line breaks, and structure. " +
                "Return ONLY the extracted text, no explanations or additional commentary."
            ));
            
            // User message with image
            List<ChatMessageContentItem> contentItems = new ArrayList<>();
            contentItems.add(new ChatMessageTextContentItem(
                "Extract all text from this document image. Return the exact text as it appears in the document."
            ));
            contentItems.add(new ChatMessageImageContentItem(
                new ChatMessageImageUrl("data:" + mimeType + ";base64," + base64Image)
            ));
            
            messages.add(new ChatRequestUserMessage(contentItems));
            
            // Call GPT-4o Vision
            System.out.println("[DocumentAIService] Calling GPT-4o Vision API...");
            ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
                    .setMaxTokens(4000)
                    .setTemperature(0.0);
            
            ChatCompletions response = openAIClient.getChatCompletions(deploymentName, options);
            String extractedText = response.getChoices().get(0).getMessage().getContent();
            
            System.out.println("[DocumentAIService] ✓ GPT-4o Vision OCR completed - " + extractedText.length() + " chars");
            System.out.println("\n[FULL OCR TEXT START]");
            System.out.println(extractedText);
            System.out.println("[FULL OCR TEXT END]\n");
            
            return extractedText.trim();
            
        } catch (Exception e) {
            System.err.println("[DocumentAIService] ⚠ OCR extraction failed: " + e.getMessage());
            e.printStackTrace();
            return ""; // Return empty string on OCR failure
        }
    }
}