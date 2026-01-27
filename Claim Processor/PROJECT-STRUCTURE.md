# MetLife Claim Processor - Project Explanation

## Introduction

Hey! So let me walk you through this project I built - it's a MetLife Claim Processor. Basically, it's an AI-powered system that automates the entire insurance death claim verification process. You know how insurance companies have to manually check all these documents, verify information, and detect fraud? This system does all of that automatically using Azure OpenAI's GPT-4o model.

---

## What Problem Does This Solve?

So here's the thing - in traditional insurance companies, when someone files a death claim, there's this whole manual process. Multiple people have to review the claim form, death certificate, doctor reports, police reports if applicable. They check if all the information matches, verify if the hospital is real, check policy rules - it takes hours, sometimes days. And you know what? Human errors happen. Sometimes fraudulent claims slip through, sometimes genuine claims get rejected by mistake.

So I thought, why not automate this entire thing using AI? That's where this project comes in.

**What my system does:**
- Takes all the claim documents (claim form, death certificate, doctor report, police report)
- Automatically extracts text from these documents using GPT-4o Vision - that's OCR, optical character recognition
- Uses an AI agent to analyze everything and detect fraud
- Cross-verifies information across all documents
- Checks policy rules using RAG - I'll explain that in a minute
- Even Google searches hospitals and police stations to verify they're real
- Makes a decision: Approved, Rejected, or Manual Review
- All of this happens in just a few minutes instead of hours

**My favorite features:**
1. The system checks if the policy exists first - no point processing documents if the policy doesn't exist, right?
2. It has this critical pre-validation where it makes sure the policy number and holder name from the form actually appear in the documents
3. Disease verification - if someone says death was due to a specific disease, that exact disease must be in the hospital records
4. Smart rejection handling - if a policy has been rejected twice already, the third attempt automatically goes to manual review
5. I even added a testing bypass feature - if all documents have "Verified by: Tejas Avhad" at the bottom, it skips validation. That's super helpful during development
6. The AI autonomously decides when to Google search - it's not hardcoded, the AI just figures out when it needs to verify something

---

## Dependencies & Their Purpose

### 1. Spring Boot Framework Dependencies

#### **spring-boot-starter-web** (4.0.2)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
- **Purpose**: Creates REST API endpoints for React frontend to submit claims
- **Use Case**: `ClaimController` exposes `/api/claim/submit` endpoint
- **Why**: Handles HTTP requests, JSON serialization, embedded Tomcat server

#### **spring-boot-starter-data-jpa** (4.0.2)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dTechnologies and Dependencies I Used

Let me tell you about all the technologies I used and why I chose each one.

### Core Framework - Spring Boot

So the whole backend is built using **Spring Boot 4.0.2** with **Java 17**. I chose Spring Boot because it makes building REST APIs super easy and it has excellent support for all the Azure services I needed.

**spring-boot-starter-web** - This is for creating REST APIs. My React frontend needs to send claim data to the backend, right? So this dependency gives me everything - the embedded Tomcat server, handles HTTP requests, converts Java objects to JSON automatically. My `ClaimController` uses this to expose the `/api/claim/submit` endpoint that React calls.

**spring-boot-starter-data-jpa** - This is for database operations. Instead of writing raw SQL queries, I just use JPA repositories. I have `ClaimRepository` and `PolicyRepository` - they give me methods like `findById()`, `save()`, `countByPolicyNumberAndClaimStatus()` without writing any SQL. It's much cleaner and less error-prone.

### Azure Services Integration

**azure-storage-blob (version 12.28.1)** - When users upload claim documents, I need to store them somewhere secure, right? So I'm using Azure Blob Storage. This dependency gives me the SDK to upload files to Azure. I created a container called `claims-documents` and my `BlobStorageService` uploads all PDFs and images there. Each file gets a unique URL that I can use later.

**azure-ai-openai (version 1.0.0-beta.10)** - This is specifically for GPT-4o Vision. I use this for OCR - optical character recognition. When someone uploads a scanned death certificate or doctor report, I need to extract the text from it. GPT-4o Vision is amazing at this. I send the image as Base64, and it returns all the text. No need for traditional OCR services.

**mssql-jdbc** - This connects my application to Azure SQL Database. All my policy data and claim records are stored in a database called `MetLifeTejas` on `metlife.database.windows.net`. This driver handles all the SQL Server specific stuff.

### LangChain4j - The AI Magic

Now this is where it gets really interesting. **LangChain4j version 0.35.0** - this is a Java framework for building AI agents. Let me explain what AI agents are.

**langchain4j core** - This is the main framework. It lets me define an AI agent interface. So I created a `ClaimAgent` interface, and LangChain4j handles all the complex stuff - calling the AI model, managing the conversation, executing tools. It's like having a smart assistant that can use different tools to get its job done.

**langchain4j-azure-open-ai** - This connects LangChain4j to Azure OpenAI. I'm using GPT-4o model - that's the latest and most capable model. It has a 128k token context window, which means it can read and analyze huge amounts of text. Perfect for analyzing multiple documents at once.

**langchain4j-embeddings** - This is for RAG, which I'll explain in detail later. But basically, it converts text into vector embeddings using Azure OpenAI's `text-embedding-ada-002` model. These embeddings let me do semantic search - finding relevant policy rules based on meaning, not just keyword matching.

**langchain4j-google-custom-search-engine** - This gives my AI agent the ability to Google search! So when the AI sees a hospital name in the doctor report, it can autonomously decide "let me verify if this hospital actually exists" and do a Google search. It's not hardcoded - the AI decides when it needs to search. Pretty cool, right?

### Why I Chose These

You might ask, why Azure OpenAI and not regular OpenAI? Well, for enterprise applications, Azure OpenAI is better because:
- It's compliant with enterprise security standards
- Data doesn't leave your Azure region
- Better SLA guarantees
- Integrates seamlessly with other Azure services

And why LangChain4j? Because building AI agents from scratch is complex. LangChain4j handles:
- Tool calling protocol
- Conversation management
- Error handling
- Retry logic
All I had to do was define what tools the AI should have access to, and it handles everything else.n Resource Sharing config
│   │
│   ├── controller/
│   │   └── ClaimController.java            # REST API endpoint
│   │
│   ├── dto/
│   │   ├── ClaimRequest.java               # API request model
│   │   └── ClaimResponse.java              # API response model
│   │
│   ├── model/
│   │   ├── Claim.java                      # Database entity (claims table)
│   │   └── Policy.java                     # Database entity (policies table)
│   │
│   ├── repository/
│   │   ├── ClaimRepository.java            # JPA repository for claims
│   │   └── PolicyRepository.java           # JPA repository for policies
│   │
│   └── service/
│       ├── PolicyRuleService.java          # Main business logic service
│       ├── ClaimAiAgentService.java        # LangChain4j AI agent builder
│       ├── ClaimAgent.java                 # AI agent interface
│       ├── DocumentAIService.java          # GPT-4o Vision OCR service
│       ├── BlobStorageService.java         # Azure Blob Storage service
│       ├── PolicyTool.java                 # AI tool: Get policy from DB
│       └── PolicyRulesRagTool.java         # AI tool: RAG for policy rules
│
├── src/main/resources/
│   ├── application.properties              # Spring Boot configuration
│   ├── tomcat.properties                   # Tomcat server settings
│
└── target/
    └── claim-processor-0.0.1-SNAPSHOT.jar  # Built JAR file
```

---

## Class Communication & Architecture

### Request Flow Diagram

```
User (React Frontend)
    ↓
    | HTTP POST /api/claim/submit
    | (FormData: files + claim info)
    ↓
ClaimController
    ↓
    | calls evaluateClaim()
    ↓
PolicyRuleService (Main Orchestrator)
    ↓
    |──→ PolicyRepository.findById()
    |    (Check if policy exists & is ACTIVE)
    |
    |──→ ClaimRepository.countByPolicyNumberAndClaimStatus()
    |    (Count previous rejections - 3rd attempt → MANUAL_REVIEW)
    |
    |──→ BlobStorageService.uploadFile()
    |    (Upload documents to Azure Blob Storage)
    |
    |──→ DocumentAIService.extractTextFromImage()
    |    ├─→ Azure OpenAI Client
    |    └─→ GPT-4o Vision (OCR extraction)
    |
    |──→ Critical Pre-Validation
    |    (Check policy number & holder name in OCR text)
    |    If mismatch → return REJECTED immediately
    |
    |──→ ClaimAiAgentService.analyzeClaim()
         ├─→ Build AI Agent with tools
         │   ├─→ PolicyTool (get policy from DB)
         │   ├─→ PolicyRulesRagTool (semantic search)
         │   └─→ WebSearchEngine (Google Search)
         │
         └─→ ClaimAgent.analyze()
             ├─→ AI uses PolicyTool.getPolicy()
             ├─→ AI uses PolicyRulesRagTool.searchRules()
             ├─→ AI uses webSearch() for hospitals
             └─→ Returns decision + reasoning
    ↓
ClaimRepository.save()
    ↓
Return ClaimResponse to React
```

---

### Detailed Class Communication

#### 1. **ClaimController** → **PolicyRuleService**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/controller/ClaimController.java](src/main/java/com/tejas/metlife/claimprocessor/controller/ClaimController.java)

```java
@PostMapping("/api/claim/submit")
public ResponseEntity<ClaimResponse> submitClaim(@ModelAttribute ClaimRequest request) {
    ClaimResponse response = policyRuleService.evaluateClaim(request);
    return ResponseEntity.ok(response);  // Always HTTP 200
}
```

**Communication:**
- Controller receives HTTP request from React
- Extracts `ClaimRequest` from `multipart/form-data`
- Calls `PolicyRuleService.evaluateClaim()`
- Returns JSON response

---

#### 2. **PolicyRuleService** → **PolicyRepository**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRuleService.java](src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRuleService.java)

**Lines 118-145: Policy Existence Check**
```java
// STEP 1: Check if policy exists FIRST
Optional<Policy> policyOpt = policyRepository.findById(policyNumber);
if (policyOpt.isEmpty()) {
    return new ClaimResponse("REJECTED", "Policy not found", claimRef);
}

Policy policy = policyOpt.get();
if (!"ACTIVE".equals(policy.getStatus())) {
    return new ClaimResponse("REJECTED", "Policy is not active", claimRef);
}
```

**Communication:**
- Service asks repository to find policy by ID
- Repository queries Azure SQL Database
- If not found/not active → immediate rejection

---

#### 3. **PolicyRuleService** → **ClaimRepository**

**Lines 147-169: Multiple Rejection Tracking**
```java
// Check how many times this policy has been rejected
long rejectionCount = claimRepository.countByPolicyNumberAndClaimStatus(
    policyNumber, "REJECTED"
);

if (rejectionCount >= 2) {
    // 3rd or more attempt - send to manual review
    policy.setStatus("UNDER_REVIEW");
    policyRepository.save(policy);
    
    Claim claim = new Claim(policyNumber, "MANUAL_REVIEW", 
        "Policy has been rejected multiple times. Manual review required.");
    claimRepository.save(claim);
    
    return new ClaimResponse("MANUAL_REVIEW", 
        "Your claim requires manual review due to previous rejections.", claimRef);
}
```

**Communication:**
- Service counts previous rejections using custom query
- If 3rd+ attempt → saves claim as MANUAL_REVIEW
- Updates policy status to UNDER_REVIEW

---

#### 4. **PolicyRuleService** → **BlobStorageService**

**Lines 171-215: Document Upload**
```java
// Upload files to Azure Blob Storage
String claimFormUrl = null;
if (claimForm != null && !claimForm.isEmpty()) {
    claimFormUrl = blobStorageService.uploadFile(claimForm, policyNumber, "claim-form");
}
// ... same for death certificate, doctor report, police report
```

**Communication:**
- Service sends each file to BlobStorageService
- BlobStorageService uploads to Azure Blob Storage container
- Returns URL for each uploaded document

---

#### 5. **BlobStorageService** → **Azure Blob Storage**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/BlobStorageService.java](src/main/java/com/tejas/metlife/claimprocessor/service/BlobStorageService.java)

```java
public String uploadFile(MultipartFile file, String policyNumber, String docType) {
    String blobName = policyNumber + "/" + docType + "_" + 
                      System.currentTimeMillis() + "_" + file.getOriginalFilename();
    
    BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
    blobClient.upload(file.getInputStream(), file.getSize(), true);
    
    return blobClient.getBlobUrl();
}
```

**Communication:**
- Creates unique blob name with policy number folder
- Uses Azure SDK to upload file
- Returns public URL of uploaded file

---

#### 6. **PolicyRuleService** → **DocumentAIService**

**Lines 238-311: OCR Extraction**
```java
// Extract text from documents using GPT-4o Vision
String claimFormText = "";
if (claimFormUrl != null) {
    claimFormText = documentAIService.extractTextFromImage(claimFormUrl);
}
// ... same for all documents
```

**Communication:**
- Service sends document URL to DocumentAIService
- DocumentAIService performs OCR using GPT-4o Vision
- Returns extracted text

---

#### 7. **DocumentAIService** → **Azure OpenAI GPT-4o Vision**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/DocumentAIService.java](src/main/java/com/tejas/metlife/claimprocessor/service/DocumentAIService.java)

```java
public String extractTextFromImage(String imageUrl) {
    // Download image
    byte[] imageBytes = downloadImage(imageUrl);
    
    // Convert to Base64
    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
    String dataUrl = "data:image/jpeg;base64," + base64Image;
    
    // Create OCR prompt
    ChatRequestSystemMessage systemMsg = new ChatRequestSystemMessage(
        "Extract all text from this image. Return only the text content."
    );
    
    ChatMessageImageContentItem imageContent = 
        new ChatMessageImageContentItem(new ChatMessageImageUrl(dataUrl));
    
    ChatRequestUserMessage userMsg = new ChatRequestUserMessage(
        new ChatMessageImageContentItem(imageContent)
    );
    
    // Call GPT-4o Vision
    ChatCompletionsOptions options = new ChatCompletionsOptions(
        Arrays.asList(systemMsg, userMsg)
    );
    options.setMaxTokens(4000);
    options.setTemperature(0.0);  // Deterministic output
    
    ChatCompletions completions = openAIClient.getChatCompletions(
        "gpt-4o", options
    );
    
    return completions.getChoices().get(0).getMessage().getContent();
}
```

**Communication:**
- Downloads image from Azure Blob Storage
- Encodes image to Base64 data URL
- Sends to Azure OpenAI GPT-4o Vision endpoint
- Returns extracted text

---

#### 8. **PolicyRuleService** → **ClaimAiAgentService**

**Lines 367-374: AI Analysis**
```java
// Call AI agent for fraud detection
String decision = claimAiAgentService.analyzeClaim(
    claimFormText,
    deathCertText,
    doctorReportText,
    policeReportText,
    policyNumber,
    filledFormInfo
);
```

**Communication:**
- Service passes all extracted text and policy info
- ClaimAiAgentService builds and executes AI agent
- Returns AI decision with reasoning

---

#### 9. **ClaimAiAgentService** → **LangChain4j AI Agent**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAiAgentService.java](src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAiAgentService.java)

```java
public String analyzeClaim(String claimFormText, String deathCertText, 
                          String doctorReportText, String policeReportText,
                          String policyNumber, String filledFormInfo) {
    
    // Build AI chat model with Azure OpenAI
    AzureOpenAiChatModel chatModel = AzureOpenAiChatModel.builder()
        .endpoint(azureOpenAIEndpoint)
        .apiKey(azureOpenAIKey)
        .deploymentName("gpt-4o")
        .temperature(0.3)
        .build();
    
    // Build embedding model for RAG
    AzureOpenAiEmbeddingModel embeddingModel = AzureOpenAiEmbeddingModel.builder()
        .endpoint(azureOpenAIEndpoint)
        .apiKey(azureOpenAIKey)
        .deploymentName("text-embedding-ada-002")
        .build();
    
    // Configure web search tool
    GoogleCustomWebSearchEngine webSearchEngine = 
        GoogleCustomWebSearchEngine.builder()
            .apiKey(googleSearchApiKey)
            .csi(googleSearchEngineId)
            .build();
    
    // Create AI agent with tools
    ClaimAgent agent = AiServices.builder(ClaimAgent.class)
        .chatLanguageModel(chatModel)
        .tools(
            new PolicyTool(policyRepository),
            new PolicyRulesRagTool(embeddingModel),
            webSearchEngine
        )
        .build();
    
    // Execute AI agent
    String prompt = String.format("""
        Filled Form Information:
        %s
        
        Claim Form Text:
        %s
        
        Death Certificate Text:
        %s
        
        Doctor Report Text:
        %s
        
        Police Report Text:
        %s
        
        Policy Number: %s
        
        Analyze this claim for fraud.
        """, filledFormInfo, claimFormText, deathCertText, 
             doctorReportText, policeReportText, policyNumber);
    
    return agent.analyze(prompt);
}
```

**Communication:**
- Creates Azure OpenAI chat model connection
- Creates embedding model for RAG
- Creates web search engine
- Builds AI agent with 3 tools (PolicyTool, PolicyRulesRagTool, WebSearchEngine)
- Executes agent with prompt containing all document text
- AI agent calls tools autonomously during analysis

---

#### 10. **AI Agent** → **PolicyTool**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/PolicyTool.java](src/main/java/com/tejas/metlife/claimprocessor/service/PolicyTool.java)

```java
@Tool("Get policy details from database by policy number")
public String getPolicy(String policyNumber) {
    Optional<Policy> policyOpt = policyRepository.findById(policyNumber);
    
    if (policyOpt.isEmpty()) {
        return "Policy not found";
    }
    
    Policy policy = policyOpt.get();
    return String.format("""
        Policy Number: %s
        Holder Name: %s
        Coverage Amount: %d
        Status: %s
        Premium: %d
        Start Date: %s
        """, policy.getPolicyNumber(), policy.getHolderName(),
             policy.getCoverageAmount(), policy.getStatus(),
             policy.getPremiumAmount(), policy.getStartDate());
}
```

**Communication:**
- AI agent calls this tool when it needs policy details
- Tool queries database via PolicyRepository
- Returns formatted policy information to AI

---

#### 11. **AI Agent** → **PolicyRulesRagTool**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRulesRagTool.java](src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRulesRagTool.java)

```java
@Tool("Search policy rules knowledge base for relevant rules")
public String searchRules(String query) {
    // Create embedding for query
    Response<Embedding> queryEmbedding = embeddingModel.embed(query);
    
    // Semantic search in vector store
    List<EmbeddingMatch<TextSegment>> matches = 
        embeddingStore.findRelevant(queryEmbedding.content(), 5);
    
    // Return relevant rules
    return matches.stream()
        .map(match -> match.embedded().text())
        .collect(Collectors.joining("\n\n"));
}
```

**Communication:**
- AI agent calls this tool with natural language query
- Tool converts query to embedding vector
- Searches InMemoryEmbeddingStore for similar policy rules
- Returns top 5 most relevant rules to AI

---

#### 12. **AI Agent** → **WebSearchEngine**

**LangChain4j Built-in Tool**

```java
// AI agent autonomously calls webSearch() when needed
// Example: AI sees "Apollo Hospital" in doctor report
// AI internally executes: webSearch("Apollo Hospital Mumbai")
// Returns: Google search results proving hospital exists
```

**Communication:**
- AI decides when to search (not hardcoded)
- LangChain4j GoogleCustomWebSearchEngine executes search
- Returns search results to AI for verification

---

## API Connectivity

### React Frontend ↔ Spring Boot Backend

#### Architecture:
```
React (Frontend)                    Spring Boot (Backend)
Port: 3000 (dev)                    Port: 8080
Static Web App (prod)               App Service (prod)
│                                   │
│  HTTP POST Request                │
│  ──────────────────────────────→  │
│  URL: /api/claim/submit           │
│  Content-Type: multipart/form-data│
│  Body: FormData                   │
│                                   │
│                                   │ Process Claim
│                                   │ (OCR, AI, Database)
│                                   │
│  ←──────────────────────────────  │
│  HTTP 200 OK Response             │
│  Content-Type: application/json   │
│  Body: ClaimResponse              │
│                                   │
```

---

### React API Call Implementation

**React Frontend Code:**

```javascript
// src/services/claimService.js
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const submitClaim = async (formData) => {
  try {
    // HTTP POST request to Spring Boot
    const response = await axios.post(
      `${API_BASE_URL}/api/claim/submit`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    
    // Response format: { status, message, claimReference }
    return response.data;
    
  } catch (error) {
    console.error('Error submitting claim:', error);
    throw error;
  }
};

// Usage in React component
const handleSubmit = async (e) => {
  e.preventDefault();
  
  // Create FormData
  const data = new FormData();
  data.append('policyNumber', policyNumber);
  data.append('policyHolderName', holderName);
  data.append('causeOfDeath', causeOfDeath);
  data.append('claimForm', claimFormFile);
  data.append('deathCertificate', deathCertFile);
  
  // Call Spring Boot API
  const result = await submitClaim(data);
  
  if (result.status === 'APPROVED') {
    alert('Claim Approved!');
  } else if (result.status === 'REJECTED') {
    alert(`Rejected: ${result.message}`);
  }
};
```

---

### Spring Boot REST Controller

**Spring Boot Backend Code:**

```java
// ClaimController.java
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend.com"})
public class ClaimController {
    
    @Autowired
    private PolicyRuleService policyRuleService;
    
    @PostMapping("/claim/submit")
    public ResponseEntity<ClaimResponse> submitClaim(
        @ModelAttribute ClaimRequest request
    ) {
        // Process claim
        ClaimResponse response = policyRuleService.evaluateClaim(request);
        
        // Always return HTTP 200 (frontend reads response.status)
        return ResponseEntity.ok(response);
    }
}
```

---

### Request/Response Format

**Request (from React):**
```
POST /api/claim/submit HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
Origin: http://localhost:3000

------WebKitFormBoundary
Content-Disposition: form-data; name="policyNumber"

POL1000
------WebKitFormBoundary
Content-Disposition: form-data; name="policyHolderName"

Sanket Patil
------WebKitFormBoundary
Content-Disposition: form-data; name="claimForm"; filename="claim.pdf"
Content-Type: application/pdf

[Binary PDF data]
------WebKitFormBoundary--
```

**Response (from Spring Boot):**
```json
{
  "status": "APPROVED",
  "message": "Claim approved based on verification",
  "claimReference": "CLM-2026-1234"
}
```

**Possible Status Values:**
- `APPROVED` - Claim is valid, payment authorized
- `REJECTED` - Claim has issues (fraud detected, info mismatch)
- `MANUAL_REVIEW` - Requires human review (3rd rejection, AI failure)

---

### CORS Configuration

**Why CORS is needed:**
- React runs on `localhost:3000` (development)
- Spring Boot runs on `localhost:8080`
- Browsers block cross-origin requests by default
- CORS allows React to call Spring Boot APIs

**Spring Boot CORS Configuration:**

```java
// CorsConfig.java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow React frontend origins
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // Development
            "https://metlife-claims.azurestaticapps.net"  // Production
        ));
        
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = 
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
```

---

### Who Uses Whose API?

**React USES Spring Boot API** ✅
- React is the client (frontend)
- Spring Boot is the server (backend)
- React sends HTTP requests TO Spring Boot
- Spring Boot responds WITH data

**Spring Boot DOES NOT USE React API** ❌
- Spring Boot doesn't call React
- React is just HTML/CSS/JS files served to browser
- Spring Boot only receives requests and sends responses

**External APIs Spring Boot Uses:**
1. **Azure OpenAI API** - For GPT-4o chat and embeddings
2. **Azure Blob Storage API** - For document storage
3. **Azure SQL Database** - For data persistence
4. **Google Custom Search API** - For web verification

---

## RAG Implementation

### What is RAG (Retrieval-Augmented Generation)?

**Definition:** RAG combines semantic search (retrieval) with AI generation to answer questions using relevant knowledge.

**Without RAG:**
```
User: "Is suicide covered?"
AI: "I don't know, I wasn't trained on your policy rules"
```

**With RAG:**
```
User: "Is suicide covered?"
RAG System: 
  1. Searches knowledge base for "suicide coverage"
  2. Finds relevant rule: "Suicide within 2 years is excluded"
  3. Passes rule to AI
AI: "Suicide is excluded if it occurs within 2 years of policy start"
```

---

### RAG Architecture in This Project

```
Policy Rules Text Files
(9 categories in src/main/resources/policy-rules/)
│
├─→ eligibility.txt
├─→ death-verification.txt
├─→ claim-amount.txt
├─→ fraud-indicators.txt
├─→ nominee-verification.txt
├─→ documentation.txt
├─→ time-limits.txt
├─→ exclusions.txt
└─→ submission-process.txt
    ↓
    | Load at startup
    ↓
Split into chunks (500 chars each)
    ↓
    | Convert to embeddings
    | (Azure OpenAI text-embedding-ada-002)
    ↓
Store in InMemoryEmbeddingStore
(Vector database)
    ↓
    | When AI needs rules
    ↓
AI Agent → PolicyRulesRagTool.searchRules("exclusions for disease death")
    ↓
    | Convert query to embedding
    ↓
Semantic Search (cosine similarity)
    ↓
    | Return top 5 most relevant rules
    ↓
AI uses rules to make decision
```

---

### RAG Implementation Code

#### 1. **Loading Policy Rules**

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRulesRagTool.java](src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRulesRagTool.java)

```java
@PostConstruct
public void loadPolicyRules() {
    // List of policy rule files
    String[] ruleFiles = {
        "eligibility.txt",
        "death-verification.txt",
        "claim-amount.txt",
        "fraud-indicators.txt",
        "nominee-verification.txt",
        "documentation.txt",
        "time-limits.txt",
        "exclusions.txt",
        "submission-process.txt"
    };
    
    // Load each file
    for (String fileName : ruleFiles) {
        try {
            Resource resource = new ClassPathResource("policy-rules/" + fileName);
            String content = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
            
            // Split into chunks (500 chars with 100 char overlap)
            List<TextSegment> segments = new RecursiveCharacterTextSplitter(
                500,  // chunk size
                100   // overlap
            ).split(content);
            
            // Generate embeddings for each chunk
            for (TextSegment segment : segments) {
                Response<Embedding> embedding = embeddingModel.embed(segment.text());
                embeddingStore.add(embedding.content(), segment);
            }
            
        } catch (IOException e) {
            System.err.println("Error loading " + fileName);
        }
    }
    
    System.out.println("✓ RAG knowledge base loaded with " + 
                       embeddingStore.size() + " segments");
}
```

**What happens:**
1. Reads 9 policy rule text files
2. Splits each file into 500-character chunks with 100-char overlap
3. Converts each chunk to embedding vector (1536 dimensions)
4. Stores vector + text in InMemoryEmbeddingStore

---

#### 2. **Semantic Search Tool**

```java
@Tool("Search policy rules knowledge base for relevant rules")
public String searchRules(String query) {
    // Convert query to embedding
    Response<Embedding> queryEmbedding = embeddingModel.embed(query);
    
    // Find top 5 similar chunks (cosine similarity)
    List<EmbeddingMatch<TextSegment>> matches = 
        embeddingStore.findRelevant(queryEmbedding.content(), 5);
    
    // Return concatenated text
    return matches.stream()
        .map(match -> match.embedded().text())
        .collect(Collectors.joining("\n\n"));
}
```

**How it works:**
1. AI agent calls `searchRules("exclusions for disease death")`
2. Query converted to embedding vector
3. Vector compared to all stored vectors (cosine similarity)
4. Top 5 most similar chunks returned
5. AI reads returned rules and uses them in decision

---

### Example RAG Knowledge Base

**File:** `src/main/resources/policy-rules/exclusions.txt`

```text
# Policy Exclusions

The following circumstances are NOT covered under this policy:

1. Suicide within 2 years of policy start date
2. Death due to pre-existing conditions not disclosed at policy purchase
3. Death occurring in war zones or during active combat
4. Death from participation in illegal activities
5. Death from self-inflicted injuries (except proven mental illness)
6. Death from drug overdose (non-prescribed substances)

Exclusions apply regardless of claim amount or nominee relationship.
```

**When AI searches for:** "Is suicide covered?"

**RAG returns:**
```
1. Suicide within 2 years of policy start date
```

**AI uses this to respond:** "Suicide is excluded if it occurs within 2 years of policy purchase"

---

### Why RAG is Better Than Hardcoding

**Without RAG (Hardcoded):**
```java
if (causeOfDeath.contains("suicide")) {
    return "REJECTED";  // Too simplistic!
}
```
- No context awareness
- Can't handle "suicide after 3 years" (which IS covered)
- Requires code changes for every rule update

**With RAG:**
- AI understands context: "suicide after 2 years" vs "suicide within 2 years"
- Rules can be updated by editing text files (no code changes)
- Handles complex multi-condition rules
- AI can combine multiple rules in reasoning

---

## LangChain4j Integration

### What is LangChain4j?

**LangChain4j** is a Java framework for building AI applications with:
- **AI Agents** - AI that can use tools autonomously
- **Tool Integration** - Give AI access to databases, APIs, web search
- **Memory** - Conversation history
- **Chains** - Multi-step workflows

---

### How LangChain4j Connects Components

```
Application Code
    ↓
    | Define AI Agent Interface
    ↓
ClaimAgent.java (@SystemMessage, @UserMessage)
    ↓
    | AiServices.builder() creates agent
    ↓
LangChain4j Core
    ├─→ ChatLanguageModel (Azure OpenAI GPT-4o)
    ├─→ EmbeddingModel (text-embedding-ada-002)
    └─→ Tools
        ├─→ PolicyTool (@Tool annotation)
        ├─→ PolicyRulesRagTool (@Tool annotation)
        └─→ WebSearchEngine (built-in)
    ↓
    | Agent executes autonomously
    ↓
Agent decides which tools to call
    ↓
Returns final answer
```

---

### AI Agent Definition

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAgent.java](src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAgent.java)

```java
public interface ClaimAgent {
    
    @SystemMessage("""
        You are an expert claim fraud detection AI for MetLife insurance.
        
        TESTING BYPASS CLAUSE:
        BEFORE ANY OTHER ANALYSIS, check if ALL uploaded documents contain 
        'Verified by: Tejas Avhad' at the bottom. If present, skip data 
        mismatch checks and only verify:
        1. Policy status is ACTIVE
        2. Policy number matches
        3. Holder name matches
        If all match, APPROVE the claim.
        
        YOUR ANALYSIS PROCESS:
        1. Verify filled form information matches extracted document text
        2. Verify policy database information matches documents
        3. Cross-verify information across all documents
        4. Check policy rules using searchRules() tool
        5. Use webSearch() to verify hospitals/police stations
        
        CRITICAL FOR DISEASE DEATHS:
        If cause of death is 'Any Disease' or specific disease name:
        The EXACT disease name must appear in BOTH filled form AND 
        hospital/doctor report. Disease mismatch is FRAUD.
        
        FRAUD INDICATORS:
        - Name mismatches across documents
        - Disease name doesn't match hospital records
        - Fake hospital or police station names
        - Timeline inconsistencies
        - Information contradictions
        
        DECISION FORMAT:
        DECISION: [APPROVED/REJECTED]
        REASON: [Detailed explanation]
        """)
    
    @UserMessage("""
        Analyze this claim:
        {{claimData}}
        """)
    String analyze(String claimData);
}
```

**How it works:**
- `@SystemMessage` = AI's instructions (rules, guidelines)
- `@UserMessage` = Prompt template with variable `{{claimData}}`
- `analyze()` method executes AI agent

---

### Building the AI Agent

**File:** [src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAiAgentService.java](src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAiAgentService.java)

```java
// Create Azure OpenAI chat model
AzureOpenAiChatModel chatModel = AzureOpenAiChatModel.builder()
    .endpoint("https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/")
    .apiKey(azureOpenAIKey)
    .deploymentName("gpt-4o")
    .temperature(0.3)  // Low temperature for consistent fraud detection
    .maxTokens(2000)
    .build();

// Create embedding model for RAG
AzureOpenAiEmbeddingModel embeddingModel = AzureOpenAiEmbeddingModel.builder()
    .endpoint("https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/")
    .apiKey(azureOpenAIKey)
    .deploymentName("text-embedding-ada-002")
    .build();

// Create web search engine
GoogleCustomWebSearchEngine webSearch = GoogleCustomWebSearchEngine.builder()
    .apiKey(googleSearchApiKey)
    .csi("4436b4b468f9941c8")  // Custom Search Engine ID
    .build();

// Build AI agent with all tools
ClaimAgent agent = AiServices.builder(ClaimAgent.class)
    .chatLanguageModel(chatModel)
    .tools(
        new PolicyTool(policyRepository),
        new PolicyRulesRagTool(embeddingModel),
        webSearch
    )
    .build();

// Execute agent
String result = agent.analyze(claimData);
```

**LangChain4j magic:**
- Automatically detects `@Tool` annotated methods
- Generates tool descriptions for AI
- Handles tool calling protocol
- Manages conversation flow
- Returns final answer

---

### Tool Calling Flow

```
1. User: "Analyze this claim: [claim data]"
   ↓
2. LangChain4j → Azure OpenAI GPT-4o
   Prompt: "You are fraud detection AI... Analyze this claim..."
   ↓
3. GPT-4o thinks: "I need policy details"
   Returns: TOOL_CALL[getPolicy, policyNumber=POL1000]
   ↓
4. LangChain4j intercepts tool call
   Executes: PolicyTool.getPolicy("POL1000")
   Returns policy details to GPT-4o
   ↓
5. GPT-4o: "Now I need policy rules about exclusions"
   Returns: TOOL_CALL[searchRules, query="exclusions for disease"]
   ↓
6. LangChain4j executes: PolicyRulesRagTool.searchRules(...)
   Returns relevant rules to GPT-4o
   ↓
7. GPT-4o: "I see 'Apollo Hospital' - let me verify"
   Returns: TOOL_CALL[webSearch, query="Apollo Hospital Mumbai"]
   ↓
8. LangChain4j executes Google search
   Returns results to GPT-4o
   ↓
9. GPT-4o: "I have all information needed"
   Returns: "DECISION: APPROVED\nREASON: All information verified..."
   ↓
10. LangChain4j returns final answer to application
```

**Key point:** AI decides which tools to call and when - not hardcoded!

---

## Docker Configuration

### What is Docker?

**Docker** packages your application with all dependencies into a **container** that runs consistently anywhere:

- **Without Docker:** "It works on my machine!" 🤷
- **With Docker:** "It works in my container, so it works everywhere!" ✅

**Benefits:**
1. **Consistency** - Same behavior on Mac, Windows, Linux, Cloud
2. **Isolation** - App runs in its own environment
3. **Portability** - Easy deployment to any server
4. **Scalability** - Run multiple containers

---

### Dockerfile Explanation

**File:** [Dockerfile](Dockerfile)

```dockerfile
# ============================================
# STAGE 1: Build Stage (Compile Java Code)
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory inside container
WORKDIR /app

# Copy Maven files first (for caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR file (skip tests for faster build)
RUN mvn clean package -DskipTests

# ============================================
# STAGE 2: Runtime Stage (Run Application)
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to spring user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port 8080
EXPOSE 8080

# Health check (verify app is running)
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Dockerfile Breakdown

#### **Multi-Stage Build**

**Why 2 stages?**

**Stage 1 (Build):**
- Base image: `maven:3.9-eclipse-temurin-17` (600MB+)
- Contains Maven, JDK, build tools
- Compiles Java code to JAR
- Final artifact: `target/*.jar`

**Stage 2 (Runtime):**
- Base image: `eclipse-temurin:17-jre-alpine` (175MB)
- Contains only JRE (no Maven, no JDK)
- Copies JAR from Stage 1
- Much smaller final image

**Size comparison:**
- Without multi-stage: ~1.2 GB
- With multi-stage: ~200 MB

---

#### **Layer Caching Optimization**

```dockerfile
# Copy pom.xml first
COPY pom.xml .

# Download dependencies (CACHED if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code AFTER dependencies
COPY src ./src
```

**Why this order?**
- Docker caches each instruction layer
- If `pom.xml` unchanged → dependencies layer reused (saves 5+ minutes)
- Source code changes don't force re-downloading dependencies

---

#### **Security Best Practices**

```dockerfile
# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Run as non-root
USER spring:spring
```

**Why not run as root?**
- If container is compromised, attacker has limited access
- Principle of least privilege

---

#### **Health Check**

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --spider http://localhost:8080/actuator/health
```

**What it does:**
- Every 30 seconds, checks if app is responding
- Docker marks container as "healthy" or "unhealthy"
- Orchestrators (Kubernetes, Azure Container Apps) can restart unhealthy containers

---

### Building Docker Image

**Command:**
```bash
docker build -t metlife-claim-processor:latest .
```

**What happens:**
```
Step 1/15 : FROM maven:3.9-eclipse-temurin-17 AS build
 ---> Pulling image maven:3.9...
 ---> Downloaded [====================================] 100%

Step 2/15 : WORKDIR /app
 ---> Creating directory /app

Step 3/15 : COPY pom.xml .
 ---> Copying pom.xml to /app/pom.xml

Step 4/15 : RUN mvn dependency:go-offline -B
 ---> Downloading dependencies...
 ---> Downloaded 145 artifacts

Step 5/15 : COPY src ./src
 ---> Copying source files

Step 6/15 : RUN mvn clean package -DskipTests
 ---> Compiling Java files...
 ---> Building JAR: target/claim-processor-0.0.1-SNAPSHOT.jar
 ---> BUILD SUCCESS

Step 7/15 : FROM eclipse-temurin:17-jre-alpine
 ---> Pulling Alpine JRE image...

Step 8/15 : COPY --from=build /app/target/*.jar app.jar
 ---> Copying JAR from build stage

Step 15/15 : ENTRYPOINT ["java", "-jar", "app.jar"]
 ---> Successfully built a1b2c3d4e5f6
 ---> Successfully tagged metlife-claim-processor:latest
```

**Verify image:**
```bash
docker images

REPOSITORY                    TAG       SIZE
metlife-claim-processor       latest    198MB
```

---

### Running Docker Container Locally

**Using docker-compose (Recommended):**

**File:** [docker-compose.yml](docker-compose.yml)

```yaml
services:
  metlife-claim-processor:
    image: metlife-claim-processor:latest
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      # Database
      - SPRING_DATASOURCE_URL=jdbc:sqlserver://metlife.database.windows.net:1433;database=MetLifeTejas
      - SPRING_DATASOURCE_USERNAME=TejasAvhad
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      
      # Azure OpenAI Chat
      - AZURE_OPENAI_ENDPOINT=https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/
      - AZURE_OPENAI_KEY=${AZURE_OPENAI_KEY}
      
      # Azure OpenAI Embeddings
      - AZURE_OPENAI_EMBEDDING_ENDPOINT=https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/
      - AZURE_OPENAI_EMBEDDING_KEY=${AZURE_OPENAI_KEY}
      
      # Azure Blob Storage
      - AZURE_STORAGE_CONNECTION_STRING=${AZURE_STORAGE_CONNECTION_STRING}
      
      # Google Search
      - GOOGLE_SEARCH_API_KEY=${GOOGLE_SEARCH_API_KEY}
      - GOOGLE_SEARCH_ENGINE_ID=4436b4b468f9941c8
    
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    
    restart: unless-stopped
    
    networks:
      - metlife-network

networks:
  metlife-network:
    driver: bridge
```

**Commands:**

```bash
# Start application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop application
docker-compose down

# Rebuild and start
docker-compose up --build -d
```

---

## Azure Cloud Deployment

### Deployment Methods

#### **Method 1: Azure App Service (Recommended for Spring Boot)**

**What is App Service?**
- Fully managed platform (PaaS)
- No need to manage servers
- Auto-scaling, load balancing
- Built-in HTTPS, custom domains
- Easy deployment (JAR, Docker)

**Architecture:**
```
Internet
    ↓
Azure Load Balancer (HTTPS)
    ↓
App Service (Java 17 Runtime)
    ├─→ Auto-scaling (1-10 instances)
    └─→ Health monitoring
    ↓
Your Spring Boot App (Port 8080)
    ↓
    ├─→ Azure SQL Database
    ├─→ Azure Blob Storage
    ├─→ Azure OpenAI
    └─→ Google Search API
```

---

### Azure CLI Deployment Steps

#### **Prerequisites:**

```bash
# Install Azure CLI
brew install azure-cli

# Login to Azure
az login
# Opens browser for authentication
```

---

#### **Step 1: Create Resource Group**

**Resource Group** = Logical container for all resources

```bash
az group create \
  --name metlife-rg \
  --location eastus2
```

**Output:**
```json
{
  "id": "/subscriptions/.../resourceGroups/metlife-rg",
  "location": "eastus2",
  "name": "metlife-rg"
}
```

---

#### **Step 2: Create App Service Plan**

**App Service Plan** = Compute resources (CPU, RAM)

```bash
az appservice plan create \
  --name metlife-plan \
  --resource-group metlife-rg \
  --sku B2 \
  --is-linux
```

**SKU Options:**
- **B1**: 1 core, 1.75 GB RAM (~$55/month)
- **B2**: 2 cores, 3.5 GB RAM (~$110/month) ← Recommended
- **S1**: 1 core, 1.75 GB RAM (~$70/month) with staging slots
- **P1V2**: 1 core, 3.5 GB RAM (~$80/month) production-ready

---

#### **Step 3: Create Web App**

```bash
az webapp create \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --plan metlife-plan \
  --runtime "JAVA:17-java17"
```

**Output:**
```json
{
  "defaultHostName": "metlife-claim-processor.azurewebsites.net",
  "state": "Running"
}
```

**Your Backend URL:** `https://metlife-claim-processor.azurewebsites.net`

---

#### **Step 4: Configure Environment Variables**

```bash
az webapp config appsettings set \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --settings \
    SPRING_DATASOURCE_URL="jdbc:sqlserver://metlife.database.windows.net:1433;database=MetLifeTejas" \
    SPRING_DATASOURCE_USERNAME="TejasAvhad" \
    SPRING_DATASOURCE_PASSWORD="your-password" \
    AZURE_OPENAI_ENDPOINT="https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/" \
    AZURE_OPENAI_KEY="your-key" \
    AZURE_OPENAI_EMBEDDING_ENDPOINT="https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/" \
    AZURE_OPENAI_EMBEDDING_KEY="your-key" \
    AZURE_STORAGE_CONNECTION_STRING="your-connection-string" \
    GOOGLE_SEARCH_API_KEY="your-key" \
    GOOGLE_SEARCH_ENGINE_ID="4436b4b468f9941c8"
```

---

#### **Step 5: Enable CORS**

**Allow React frontend to call Spring Boot API:**

```bash
az webapp cors add \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --allowed-origins \
    "http://localhost:3000" \
    "https://your-react-app.azurestaticapps.net"
```

---

#### **Step 6: Build JAR**

```bash
mvn clean package -DskipTests
```

**Output:**
```
[INFO] Building jar: target/claim-processor-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

---

#### **Step 7: Deploy JAR to Azure**

```bash
az webapp deploy \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --src-path target/claim-processor-0.0.1-SNAPSHOT.jar \
  --type jar
```

**What happens:**
1. JAR uploaded to Azure (takes 1-2 minutes)
2. App Service extracts JAR
3. Starts Java 17 runtime
4. Runs: `java -jar app.jar`
5. Application starts on port 8080

---

#### **Step 8: Verify Deployment**

```bash
# Check application status
az webapp show \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --query "state"

# Output: "Running"

# View logs
az webapp log tail \
  --name metlife-claim-processor \
  --resource-group metlife-rg
```

**Test health endpoint:**
```bash
curl https://metlife-claim-processor.azurewebsites.net/actuator/health

# Response:
{
  "status": "UP"
}
```

**Test API:**
```bash
curl -X POST https://metlife-claim-processor.azurewebsites.net/api/claim/submit \
  -F "policyNumber=POL1000" \
  -F "claimForm=@claim.pdf"
```

---

### Using Deployment Script

**File:** [deploy-azure.sh](deploy-azure.sh)

**Make executable:**
```bash
chmod +x deploy-azure.sh
```

**Run deployment:**
```bash
./deploy-azure.sh
```

**Script output:**
```
=========================================
MetLife Claim Processor - Azure Deployment
=========================================

Step 1: Checking Azure login...
✓ Logged in as: user@example.com

Step 2: Creating Resource Group...
✓ Resource group created: metlife-rg

Step 3: Creating App Service Plan...
✓ App Service Plan created: metlife-plan (B2)

Step 4: Creating Web App...
✓ Web App created: metlife-claim-processor

Step 5: Configuring App Settings...
✓ Environment variables configured

Step 6: Enabling CORS...
✓ CORS enabled for React frontend

Step 7: Building Spring Boot application...
✓ JAR built successfully

Step 8: Deploying application...
✓ Deployment complete!

=========================================
✅ Deployment Complete!
=========================================

Backend API URL: https://metlife-claim-processor.azurewebsites.net
Health Check: https://metlife-claim-processor.azurewebsites.net/actuator/health
API Endpoint: https://metlife-claim-processor.azurewebsites.net/api/claim/submit
```

---

### Docker Deployment to Azure

#### **Method 2: Azure Container Registry + Container Apps**

**Step 1: Create Container Registry**
```bash
az acr create \
  --name metliferegistry \
  --resource-group metlife-rg \
  --sku Basic \
  --admin-enabled true
```

**Step 2: Login to ACR**
```bash
az acr login --name metliferegistry
```

**Step 3: Tag and Push Image**
```bash
# Tag image
docker tag metlife-claim-processor:latest \
  metliferegistry.azurecr.io/claim-processor:v1

# Push to ACR
docker push metliferegistry.azurecr.io/claim-processor:v1
```

**Step 4: Create Container App**
```bash
az containerapp create \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --environment metlife-env \
  --image metliferegistry.azurecr.io/claim-processor:v1 \
  --target-port 8080 \
  --ingress external \
  --registry-server metliferegistry.azurecr.io \
  --env-vars \
    SPRING_DATASOURCE_URL="jdbc:sqlserver://..." \
    AZURE_OPENAI_KEY="your-key"
```

---

### Monitoring & Troubleshooting

#### **View Real-time Logs:**
```bash
az webapp log tail \
  --name metlife-claim-processor \
  --resource-group metlife-rg
```

#### **Download Log Files:**
```bash
az webapp log download \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --log-file logs.zip
```

#### **Check Application Insights:**
```bash
# Enable Application Insights
az monitor app-insights component create \
  --app metlife-insights \
  --location eastus2 \
  --resource-group metlife-rg

# Link to Web App
az webapp config appsettings set \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --settings \
    APPINSIGHTS_INSTRUMENTATIONKEY="your-key"
```

#### **SSH into Container:**
```bash
az webapp ssh \
  --name metlife-claim-processor \
  --resource-group metlife-rg
```

---

### Cost Management

**Monthly Cost Estimate:**

| Resource | Tier | Cost |
|----------|------|------|
| App Service (B2) | 2 cores, 3.5GB | $110 |
| Azure SQL (Basic) | 5 DTUs | $5 |
| Azure Storage | Standard | $2 |
| Azure OpenAI | Pay-per-use | $50-200 |
| **Total** | | **$167-317/month** |

**Cost Optimization:**
- Use **B1** for development ($55/month)
- Stop app when not in use
- Use **Consumption Plan** for Azure Functions (if refactored)

---

### Complete Deployment Workflow

```
Local Development
    ↓
    | git commit & push
    ↓
GitHub Repository
    ↓
    | GitHub Actions (CI/CD)
    ↓
    ├─→ Build JAR
    ├─→ Run tests
    └─→ Build Docker image
    ↓
    | Push to Azure Container Registry
    ↓
Azure Container Registry
    ↓
    | Deploy to production
    ↓
Azure App Service / Container Apps
    ↓
    | Serve requests
    ↓
Production (https://metlife-claim-processor.azurewebsites.net)
```

---

## Summary

### Technology Stack:
- **Frontend:** React (Static Web App)
- **Backend:** Spring Boot 4.0.2 (Java 17)
- **AI:** Azure OpenAI GPT-4o + LangChain4j 0.35.0
- **OCR:** GPT-4o Vision
- **RAG:** text-embedding-ada-002 + InMemoryEmbeddingStore
- **Search:** Google Custom Search API
- **Database:** Azure SQL Database
- **Storage:** Azure Blob Storage
- **Containerization:** Docker multi-stage build
- **Deployment:** Azure App Service / Container Apps

### Key Features:
1. **Policy validation** before processing
2. **GPT-4o Vision OCR** for document extraction
3. **AI Agent** with 3 tools (PolicyTool, RAG, WebSearch)
4. **RAG system** with 9 policy rule categories
5. **Multi-layered fraud detection**
6. **Testing bypass** for development
7. **Multiple rejection handling** (3rd attempt → manual review)
8. **Azure cloud-ready** with Docker support

---

**Project Status:** ✅ Production Ready

**Backend URL:** https://metlife-claim-processor.azurewebsites.net

**Documentation:** Complete

---
