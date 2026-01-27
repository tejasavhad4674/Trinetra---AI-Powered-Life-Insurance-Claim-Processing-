package com.tejas.metlife.claimprocessor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejas.metlife.claimprocessor.dto.AiDecision;
import com.tejas.metlife.claimprocessor.service.agent.ClaimAgent;
import com.tejas.metlife.claimprocessor.service.tool.PolicyTool;
import com.tejas.metlife.claimprocessor.service.tool.PolicyRulesRagTool;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * LangChain4j-based AI Agent Service for Claim Fraud Detection.
 * Integrates Azure OpenAI with PolicyTool, PolicyRulesRagTool, and WebSearchTool.
 */
@Service
public class ClaimAiAgentService {

    @Value("${azure.openai.endpoint}")
    private String azureOpenAiEndpoint;

    @Value("${azure.openai.key}")
    private String azureOpenAiKey;

    @Value("${azure.openai.deployment}")
    private String azureOpenAiDeployment;

    @Value("${google.search.api.key:}")
    private String googleSearchApiKey;

    @Value("${google.search.engine.id:}")
    private String googleSearchEngineId;

    private final PolicyTool policyTool;
    private final PolicyRulesRagTool policyRulesRagTool;
    private final ObjectMapper objectMapper;
    private ClaimAgent claimAgent;

    public ClaimAiAgentService(PolicyTool policyTool, 
                               PolicyRulesRagTool policyRulesRagTool,
                               ObjectMapper objectMapper) {
        this.policyTool = policyTool;
        this.policyRulesRagTool = policyRulesRagTool;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        System.out.println("\n========== INITIALIZING LANGCHAIN4J AI AGENT ==========\n");
        
        System.out.println("[ClaimAiAgentService] Configuring Azure OpenAI Chat Model");
        System.out.println("[ClaimAiAgentService] → Endpoint: " + azureOpenAiEndpoint);
        System.out.println("[ClaimAiAgentService] → Deployment: " + azureOpenAiDeployment);
        System.out.println("[ClaimAiAgentService] → API Key: " + (azureOpenAiKey != null && !azureOpenAiKey.isEmpty() ? "✓ Configured" : "✗ MISSING"));
        
        // Configure Azure OpenAI Chat Model
        ChatLanguageModel chatModel = AzureOpenAiChatModel.builder()
                .endpoint(azureOpenAiEndpoint)
                .apiKey(azureOpenAiKey)
                .deploymentName(azureOpenAiDeployment)
                .temperature(0.7)
                .maxTokens(1500)
                .logRequestsAndResponses(false)
                .build();
        
        System.out.println("[ClaimAiAgentService] ✓ Azure OpenAI Chat Model configured successfully");

        // Build AI Services with tools
        System.out.println("[ClaimAiAgentService] Building AI Services with tools...");
        System.out.println("[ClaimAiAgentService] → PolicyTool: Fetch specific policy details from database");
        System.out.println("[ClaimAiAgentService] → PolicyRulesRagTool: Retrieve relevant policy rules using RAG");
        
        var builder = AiServices.builder(ClaimAgent.class)
                .chatLanguageModel(chatModel);

        // Add web search tool if configured
        if (googleSearchApiKey != null && !googleSearchApiKey.isEmpty() 
            && googleSearchEngineId != null && !googleSearchEngineId.isEmpty()) {
            
            System.out.println("[ClaimAiAgentService] → Google Search API Key: ✓ Configured");
            System.out.println("[ClaimAiAgentService] → Search Engine ID: " + googleSearchEngineId);
            
            WebSearchEngine webSearchEngine = GoogleCustomWebSearchEngine.builder()
                    .apiKey(googleSearchApiKey)
                    .csi(googleSearchEngineId)
                    .build();
            
            builder.tools(policyTool, policyRulesRagTool, webSearchEngine);
            System.out.println("[ClaimAiAgentService] ✓ Initialized with PolicyTool, PolicyRulesRagTool, AND WebSearchTool");
        } else {
            builder.tools(policyTool, policyRulesRagTool);
            System.out.println("[ClaimAiAgentService] ⚠ Initialized with PolicyTool and PolicyRulesRagTool (no web search - API key missing)");
        }

        claimAgent = builder.build();
        System.out.println("[ClaimAiAgentService] ✓ ClaimAgent successfully initialized and ready!");
        System.out.println("\n========== LANGCHAIN4J AI AGENT READY ==========\n");
    }

    /**
     * Analyze claim using AI agent with policy rules and web search.
     * 
     * @param extractedText OCR text from claim documents
     * @param policyNumber Policy number to fetch rules
     * @return AiDecision with decision and reason
     */
    public AiDecision analyzeClaim(String extractedText, String policyNumber) {
        try {
            System.out.println("\n========== STARTING AI AGENT ANALYSIS ==========\n");
            System.out.println("[ClaimAiAgentService] Policy Number: " + policyNumber);
            System.out.println("[ClaimAiAgentService] Extracted Text Length: " + (extractedText != null ? extractedText.length() : 0) + " chars");
            System.out.println("[ClaimAiAgentService] Calling AI Agent with PolicyTool...");
            
            // Call AI agent
            String jsonResponse = claimAgent.analyze(extractedText, policyNumber);
            
            System.out.println("[ClaimAiAgentService] AI Agent Response:\n" + jsonResponse);
            System.out.println("\n========== AI AGENT ANALYSIS COMPLETE ==========\n");
            
            // Parse JSON response
            return parseAiResponse(jsonResponse);
            
        } catch (Exception e) {
            System.err.println("\n[ClaimAiAgentService] ✗ ERROR during AI analysis: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to manual review
            return new AiDecision("MANUAL_REVIEW", 
                "AI analysis failed: " + e.getMessage() + ". Manual review required.");
        }
    }

    /**
     * Parse AI agent JSON response into AiDecision object.
     */
    private AiDecision parseAiResponse(String jsonResponse) {
        try {
            // Remove markdown code blocks if present
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();
            
            // Parse JSON
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            
            String decision = rootNode.has("decision") 
                ? rootNode.get("decision").asText() 
                : "MANUAL_REVIEW";
            
            String reason = rootNode.has("reason") 
                ? rootNode.get("reason").asText() 
                : "Unable to parse AI response";
            
            // Validate decision
            if (!decision.equals("APPROVED") && 
                !decision.equals("REJECTED") && 
                !decision.equals("MANUAL_REVIEW")) {
                decision = "MANUAL_REVIEW";
                reason = "Invalid decision from AI: " + decision + ". " + reason;
            }
            
            return new AiDecision(decision, reason);
            
        } catch (Exception e) {
            System.err.println("[ClaimAiAgentService] Failed to parse AI response: " + jsonResponse);
            e.printStackTrace();
            return new AiDecision("MANUAL_REVIEW", 
                "Failed to parse AI response. Manual review required.");
        }
    }
}
