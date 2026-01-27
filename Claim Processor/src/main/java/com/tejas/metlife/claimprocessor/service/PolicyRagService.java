package com.tejas.metlife.claimprocessor.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG Service for storing and retrieving policy rules and constraints.
 * Uses LangChain4j with Azure OpenAI embeddings and in-memory vector store.
 */
@Service
public class PolicyRagService {

    @Value("${azure.openai.embedding.endpoint}")
    private String azureOpenAiEndpoint;

    @Value("${azure.openai.embedding.key}")
    private String azureOpenAiKey;

    @Value("${azure.openai.embedding.deployment:text-embedding-ada-002}")
    private String embeddingDeployment;

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private boolean ragEnabled = false;

    @PostConstruct
    public void init() {
        System.out.println("\n========== INITIALIZING POLICY RAG SERVICE ==========\n");
        
        try {
            // Initialize Azure OpenAI Embedding Model
            System.out.println("[PolicyRagService] Configuring Azure OpenAI Embedding Model");
            System.out.println("[PolicyRagService] → Endpoint: " + azureOpenAiEndpoint);
            System.out.println("[PolicyRagService] → Deployment: " + embeddingDeployment);
            
            embeddingModel = AzureOpenAiEmbeddingModel.builder()
                    .endpoint(azureOpenAiEndpoint)
                    .apiKey(azureOpenAiKey)
                    .deploymentName(embeddingDeployment)
                    .serviceVersion("2024-02-01")
                    .timeout(java.time.Duration.ofSeconds(60))
                    .logRequestsAndResponses(false)
                    .build();
            
            System.out.println("[PolicyRagService] ✓ Embedding Model initialized");

            // Initialize In-Memory Embedding Store
            embeddingStore = new InMemoryEmbeddingStore<>();
            System.out.println("[PolicyRagService] ✓ In-Memory Vector Store initialized");

            // Load default policy rules
            loadDefaultPolicyRules();
            
            ragEnabled = true;
            System.out.println("[PolicyRagService] ✓ Policy RAG Service ready!");
        } catch (Exception e) {
            System.err.println("[PolicyRagService] ⚠ WARNING: Failed to initialize RAG service");
            System.err.println("[PolicyRagService] → Error: " + e.getMessage());
            System.err.println("[PolicyRagService] → RAG features will be DISABLED");
            System.err.println("[PolicyRagService] → To enable RAG, deploy 'text-embedding-ada-002' model in Azure OpenAI");
            ragEnabled = false;
        }
        
        System.out.println("\n========== POLICY RAG SERVICE INITIALIZATION " + (ragEnabled ? "COMPLETE" : "SKIPPED") + " ==========\n");
    }

    /**
     * Load default MetLife policy rules and constraints into vector store.
     */
    private void loadDefaultPolicyRules() {
        System.out.println("[PolicyRagService] Loading default policy rules into vector store...");

        // Define MetLife policy rules as documents
        List<String> policyRules = List.of(
            // General Policy Rules
            """
            MetLife Policy General Rules:
            - All policies must be active to process claims.
            - Policy holder must have paid all premiums up to date.
            - Claims must be filed within 30 days of the incident.
            - Claim form must be filled completely with all required documents.
            """,

            // Suicide Coverage
            """
            MetLife Suicide Coverage Rules:
            - Suicide is NOT covered within the first year of policy issuance.
            - If suicide occurs after the suicide coverage period (typically 1 year), claim may be processed.
            - Death certificate must clearly state cause as suicide.
            - Police report and medical examination required for suicide claims.
            """,

            // Accidental Death Coverage
            """
            MetLife Accidental Death Coverage Rules:
            - Accidental death is covered if policy includes accident coverage.
            - Police FIR (First Information Report) is mandatory for accidental death claims.
            - Hospital admission records and doctor reports required.
            - Accident must be verified and not related to illegal activities.
            - Death must occur within 180 days of the accident.
            """,

            // Natural Death Coverage
            """
            MetLife Natural Death Coverage Rules:
            - Natural death due to disease or medical conditions is covered.
            - Death certificate from registered medical practitioner required.
            - Hospital discharge summary or doctor's report required if hospitalized.
            - Pre-existing conditions may be excluded for first 2 years unless disclosed.
            """,

            // Disease Coverage
            """
            MetLife Disease Death Coverage Rules:
            - Death due to disease is covered under natural death coverage.
            - Medical history and treatment records required.
            - Hospital bills and discharge summary required if hospitalized.
            - Terminal illness claims require specialist doctor certification.
            """,

            // Document Requirements
            """
            MetLife Required Documents for Claims:
            - Death Certificate (mandatory for all claims)
            - Completed Claim Form with nominee details (mandatory)
            - Original Policy Document (mandatory)
            - For Accidental Death: Police FIR, Postmortem Report, Hospital Records
            - For Natural Death: Doctor's Certificate, Hospital Records (if applicable)
            - For Disease Death: Medical Records, Treatment History, Hospital Bills
            - Identity proof of nominee and claimant (mandatory)
            """,

            // Exclusions
            """
            MetLife Policy Exclusions:
            - Death due to war, terrorism, or riot (excluded)
            - Death due to drug overdose or alcohol poisoning (excluded unless accidental)
            - Self-inflicted injuries (excluded)
            - Death during illegal activities (excluded)
            - Aviation accidents (excluded unless passenger in commercial flight)
            - Pre-existing conditions not disclosed at policy purchase (may be excluded)
            """,

            // Claim Process Timeline
            """
            MetLife Claim Processing Timeline:
            - Claim notification must be given within 7 days of death.
            - All documents must be submitted within 30 days of death.
            - Claim processing takes 15-30 days after document verification.
            - If investigation required, additional 30-60 days may be needed.
            - Approved claims paid within 7 days of approval.
            """,

            // Fraud Detection Rules
            """
            MetLife Fraud Detection Guidelines:
            - Claims with fake or forged documents will be rejected.
            - Non-existent hospitals or police stations indicate fraud.
            - Inconsistent information across documents requires investigation.
            - Claims with gibberish or meaningless OCR text are suspicious.
            - Multiple claims for same policy number require verification.
            - Claims filed immediately after policy purchase require extra scrutiny.
            """
        );

        // Split documents into smaller chunks
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
        
        int segmentCount = 0;
        for (String rule : policyRules) {
            try {
                Document doc = Document.from(rule);
                List<TextSegment> segments = splitter.split(doc);
                
                // Embed and store each segment
                for (TextSegment segment : segments) {
                    try {
                        Embedding embedding = embeddingModel.embed(segment).content();
                        embeddingStore.add(embedding, segment);
                        segmentCount++;
                    } catch (Exception embedEx) {
                        System.err.println("[PolicyRagService] ⚠ Failed to embed segment: " + embedEx.getMessage());
                        // Continue with next segment
                    }
                }
            } catch (Exception docEx) {
                System.err.println("[PolicyRagService] ⚠ Failed to process policy rule: " + docEx.getMessage());
                // Continue with next rule
            }
        }

        System.out.println("[PolicyRagService] ✓ Loaded " + segmentCount + " policy rule segments into vector store");
    }

    /**
     * Retrieve relevant policy rules based on query.
     * 
     * @param query The query text (e.g., "suicide coverage", "accidental death rules")
     * @param maxResults Maximum number of relevant segments to retrieve
     * @return List of relevant policy rule text segments
     */
    public String retrieveRelevantPolicyRules(String query, int maxResults) {
        if (!ragEnabled) {
            System.out.println("[PolicyRagService] ⚠ RAG is disabled - returning fallback message");
            return "RAG service unavailable. Using general insurance policy guidelines.";
        }
        
        System.out.println("[PolicyRagService] Retrieving policy rules for query: " + query);
        
        try {
            // Embed the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // Find most relevant segments
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults);
            
            // Combine relevant segments
            String relevantRules = matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n\n"));
            
            System.out.println("[PolicyRagService] ✓ Retrieved " + matches.size() + " relevant policy rule segments");
            
            return relevantRules;
        } catch (Exception e) {
            System.err.println("[PolicyRagService] ⚠ Error retrieving policy rules: " + e.getMessage());
            return "Error retrieving policy rules. Using general insurance guidelines.";
        }
    }

    /**
     * Add custom policy rules to the vector store.
     * 
     * @param policyRule The policy rule text to add
     */
    public void addPolicyRule(String policyRule) {
        if (!ragEnabled) {
            System.out.println("[PolicyRagService] ⚠ RAG is disabled - cannot add policy rule");
            return;
        }
        
        System.out.println("[PolicyRagService] Adding new policy rule to vector store");
        
        try {
            Document doc = Document.from(policyRule);
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
            List<TextSegment> segments = splitter.split(doc);
            
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
            
            System.out.println("[PolicyRagService] ✓ Policy rule added successfully");
        } catch (Exception e) {
            System.err.println("[PolicyRagService] ⚠ Error adding policy rule: " + e.getMessage());
        }
    }
    
    /**
     * Check if RAG service is enabled and ready.
     */
    public boolean isRagEnabled() {
        return ragEnabled;
    }
}
