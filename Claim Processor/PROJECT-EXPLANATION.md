# MetLife Claim Processor - Project Explanation (As I Would Explain It Orally)

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

## Technologies and Dependencies I Used

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

An AI agent is basically an AI that can use tools autonomously. Like, you give it a task, and it figures out which tools it needs to use to complete that task. It's not like a simple chatbot that just answers questions - it can actually call functions, query databases, search the web, all by itself.

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

All I had to do was define what tools the AI should have access to, and it handles everything else.

---

## Project Structure - How I Organized Everything

Let me show you how I structured the entire project. It follows standard Spring Boot conventions, so it's easy for other developers to understand.

```
Claim Processor/
│
├── pom.xml                    # Maven configuration - all my dependencies
├── Dockerfile                 # Instructions to build Docker image
├── docker-compose.yml         # Easy way to run the app in Docker
├── deploy-azure.sh           # Script I wrote to deploy to Azure
│
├── src/main/java/com/tejas/metlife/claimprocessor/
│   │
│   ├── ClaimProcessorApplication.java    # Main entry point
│   │
│   ├── config/
│   │   └── CorsConfig.java               # CORS settings for React
│   │
│   ├── controller/
│   │   └── ClaimController.java          # REST API endpoints
│   │
│   ├── dto/
│   │   ├── ClaimRequest.java             # Request format
│   │   └── ClaimResponse.java            # Response format
│   │
│   ├── model/
│   │   ├── Claim.java                    # Database table entity
│   │   └── Policy.java                   # Database table entity
│   │
│   ├── repository/
│   │   ├── ClaimRepository.java          # Database operations for claims
│   │   └── PolicyRepository.java         # Database operations for policies
│   │
│   └── service/
│       ├── PolicyRuleService.java        # Main business logic (brain of the app)
│       ├── ClaimAiAgentService.java      # Builds the AI agent
│       ├── ClaimAgent.java               # AI agent interface definition
│       ├── DocumentAIService.java        # GPT-4o Vision OCR
│       ├── BlobStorageService.java       # Azure file uploads
│       ├── PolicyTool.java               # AI tool: fetch policy from DB
│       └── PolicyRulesRagTool.java       # AI tool: search policy rules
│
└── src/main/resources/
    ├── application.properties            # Configuration
    └── tomcat.properties/

```

So basically, I have:
- **Controllers** - handle incoming HTTP requests from React
- **Services** - all the business logic lives here
- **Repositories** - talk to the database
- **Models** - represent database tables
- **DTOs** - data transfer objects for API requests/responses
- **Config** - configuration classes like CORS

The `PolicyRuleService` is the brain - it orchestrates everything. It talks to the repositories, calls the blob storage service, uses the document AI service for OCR, and then calls the AI agent service for fraud detection.

---

## How Everything Communicates - The Complete Flow

Let me walk you through what happens when someone submits a claim. This is the complete journey from React frontend to the final decision.

### Step 1: React Frontend Sends Request

So a user fills out a form in React - enters policy number, holder name, deceased person's info, selects files (claim form, death certificate, doctor report, police report). When they click submit, React creates a FormData object and sends it to my Spring Boot backend:

```javascript
const formData = new FormData();
formData.append('policyNumber', 'POL1000');
formData.append('policyHolderName', 'Sanket Patil');
formData.append('causeOfDeath', 'Heart Attack');
formData.append('claimForm', claimFormFile);
formData.append('deathCertificate', deathCertFile);

// HTTP POST to Spring Boot
const response = await axios.post(
  'https://metlife-claim-processor.azurewebsites.net/api/claim/submit',
  formData
);
```

### Step 2: ClaimController Receives Request

My `ClaimController` receives this request at the `/api/claim/submit` endpoint:

```java
@PostMapping("/api/claim/submit")
public ResponseEntity<ClaimResponse> submitClaim(@ModelAttribute ClaimRequest request) {
    ClaimResponse response = policyRuleService.evaluateClaim(request);
    return ResponseEntity.ok(response);
}
```

It just extracts the data and immediately calls `PolicyRuleService.evaluateClaim()`. That's where all the magic happens.

### Step 3: Policy Validation First

The first thing I do is check if the policy even exists. No point processing documents if the policy doesn't exist, right?

```java
Optional<Policy> policyOpt = policyRepository.findById(policyNumber);
if (policyOpt.isEmpty()) {
    return new ClaimResponse("REJECTED", "Policy not found", claimRef);
}

Policy policy = policyOpt.get();
if (!"ACTIVE".equals(policy.getStatus())) {
    return new ClaimResponse("REJECTED", "Policy is not active", claimRef);
}
```

If the policy doesn't exist or isn't active, I reject immediately with a clear reason. No unnecessary processing.

### Step 4: Check Multiple Rejection Attempts

Here's a feature I'm really proud of. If someone's claim has been rejected twice already, and they're trying a third time, I automatically send it to manual review instead of processing it again. This prevents people from repeatedly trying to commit fraud.

```java
long rejectionCount = claimRepository.countByPolicyNumberAndClaimStatus(
    policyNumber, "REJECTED"
);

if (rejectionCount >= 2) {
    // 3rd attempt - manual review
    policy.setStatus("UNDER_REVIEW");
    Claim claim = new Claim(policyNumber, "MANUAL_REVIEW", 
        "Policy has been rejected multiple times. Manual review required.");
    claimRepository.save(claim);
    
    return new ClaimResponse("MANUAL_REVIEW", 
        "Your claim requires manual review due to previous rejections.", claimRef);
}
```

This is smart because it catches persistent fraud attempts while still giving genuine claimants a chance to correct mistakes.

### Step 5: Upload Files to Azure Blob Storage

Now I upload all the documents to Azure Blob Storage. This keeps them safe and gives me URLs to access them later:

```java
String claimFormUrl = blobStorageService.uploadFile(claimForm, policyNumber, "claim-form");
String deathCertUrl = blobStorageService.uploadFile(deathCertificate, policyNumber, "death-cert");
```

My `BlobStorageService` creates a folder structure like `POL1000/claim-form_timestamp_filename.pdf`. Clean and organized.

### Step 6: OCR - Extract Text from Documents

This is where GPT-4o Vision comes in. For each document, I send it to my `DocumentAIService`:

```java
String claimFormText = documentAIService.extractTextFromImage(claimFormUrl);
String deathCertText = documentAIService.extractTextFromImage(deathCertUrl);
String doctorReportText = documentAIService.extractTextFromImage(doctorReportUrl);
String policeReportText = documentAIService.extractTextFromImage(policeReportUrl);
```

Inside `DocumentAIService`, I'm doing something interesting. I download the image, convert it to Base64, and send it to GPT-4o Vision. Temperature is set to 0.0 which means it's deterministic - same input always gives same output. Important for consistency.

### Step 7: Critical Pre-Validation

Before I even call the AI agent (which costs money), I do a critical check. The policy number and holder name from the filled form MUST appear somewhere in the OCR extracted text:

```java
String policyNumFromForm = request.getPolicyNumber();
String holderNameFromForm = request.getPolicyHolderName();

// Combine all extracted text
String allExtractedText = (claimFormText + " " + deathCertText + " " + 
                          doctorReportText + " " + policeReportText).toLowerCase();

// Check if policy number exists
if (!allExtractedText.contains(policyNumFromForm.toLowerCase())) {
    return new ClaimResponse("REJECTED", 
        "Fraud detected: Policy number in filled form doesn't match documents", claimRef);
}

// Check if holder name exists
if (!allExtractedText.contains(holderNameFromForm.toLowerCase())) {
    return new ClaimResponse("REJECTED", 
        "Fraud detected: Policy holder name in filled form doesn't match documents", claimRef);
}
```

This catches obvious fraud attempts early. Why waste expensive AI tokens if basic info doesn't match?

### Step 8: Call the AI Agent

If everything looks good so far, I call my AI agent:

```java
String decision = claimAiAgentService.analyzeClaim(
    claimFormText,
    deathCertText,
    doctorReportText,
    policeReportText,
    policyNumber,
    filledFormInfo
);
```

---

## The AI Agent - How It Works

Now let me explain the most interesting part - the AI agent. This is where LangChain4j really shines.

### Building the AI Agent

Inside `ClaimAiAgentService`, I build an AI agent with three tools:

```java
// 1. Create the chat model
AzureOpenAiChatModel chatModel = AzureOpenAiChatModel.builder()
    .endpoint("https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/")
    .apiKey(azureOpenAIKey)
    .deploymentName("gpt-4o")
    .temperature(0.3)  // Low temperature for consistent fraud detection
    .build();

// 2. Create embedding model for RAG
AzureOpenAiEmbeddingModel embeddingModel = AzureOpenAiEmbeddingModel.builder()
    .endpoint("https://tejas-mkwctf17-eastus2.cognitiveservices.azure.com/")
    .apiKey(azureOpenAIKey)
    .deploymentName("text-embedding-ada-002")
    .build();

// 3. Create web search engine
GoogleCustomWebSearchEngine webSearch = GoogleCustomWebSearchEngine.builder()
    .apiKey(googleSearchApiKey)
    .csi("4436b4b468f9941c8")
    .build();

// 4. Build the AI agent with all tools
ClaimAgent agent = AiServices.builder(ClaimAgent.class)
    .chatLanguageModel(chatModel)
    .tools(
        new PolicyTool(policyRepository),
        new PolicyRulesRagTool(embeddingModel),
        webSearch
    )
    .build();
```

So the AI agent has access to three tools:
1. **PolicyTool** - Can query the database to get policy details
2. **PolicyRulesRagTool** - Can search through policy rules using semantic search
3. **WebSearchEngine** - Can Google search to verify hospitals/police stations

### The AI Agent Interface

I defined the AI agent's behavior in an interface:

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
        
        DECISION FORMAT:
        DECISION: [APPROVED/REJECTED]
        REASON: [Detailed explanation]
        """)
    
    @UserMessage("Analyze this claim: {{claimData}}")
    String analyze(String claimData);
}
```

The `@SystemMessage` is like the AI's instruction manual. It tells the AI:
- What its role is
- What steps to follow
- What to look for
- How to format the response

### How the AI Uses Tools

Here's what's really cool. The AI decides autonomously which tools to call. Let me give you an example:

**Scenario**: AI is analyzing a claim with "Apollo Hospital" mentioned in the doctor report.

**What happens behind the scenes:**

1. AI thinks: "I need to verify this hospital actually exists"
2. AI calls: `webSearch("Apollo Hospital Mumbai")`
3. Google returns search results proving the hospital exists
4. AI continues analysis with this information

**Another scenario**: AI needs to check if suicide is covered.

1. AI thinks: "I need to check policy rules about exclusions"
2. AI calls: `searchRules("suicide coverage exclusions")`
3. RAG system returns: "Suicide within 2 years of policy start is excluded"
4. AI checks the policy start date
5. AI uses this rule to make a decision

**The key point**: I didn't hardcode any of this. The AI figures out what tools it needs and when to use them. That's the power of AI agents!

---

## RAG - Retrieval Augmented Generation

Now let me explain RAG. This is one of the coolest features.

### What's the Problem RAG Solves?

You see, GPT-4o is trained on general knowledge up to a certain date. It doesn't know about MetLife's specific policy rules. So if I just asked it "Is this claim valid according to policy rules?", it would make stuff up or say "I don't know."

RAG solves this by:
1. Storing all policy rules in a knowledge base
2. When the AI needs info about rules, it searches this knowledge base
3. Returns relevant rules to the AI
4. AI uses these rules to make decisions

### How I Implemented RAG

I created 9 text files with different policy rule categories:

```
policy-rules/
├── eligibility.txt           - Who can claim, eligibility criteria
├── death-verification.txt    - How death must be verified
├── claim-amount.txt          - How claim amount is calculated
├── fraud-indicators.txt      - Signs of fraudulent claims
├── nominee-verification.txt  - Nominee verification requirements
├── documentation.txt         - Required documents
├── time-limits.txt          - Claim filing deadlines
├── exclusions.txt           - What's NOT covered
└── submission-process.txt   - How to submit claims
```

### Converting Text to Embeddings

At application startup, I load all these files and convert them into vector embeddings:

```java
@PostConstruct
public void loadPolicyRules() {
    String[] ruleFiles = {"eligibility.txt", "death-verification.txt", ...};
    
    for (String fileName : ruleFiles) {
        // Read file content
        String content = readFile("policy-rules/" + fileName);
        
        // Split into 500-character chunks with 100-char overlap
        List<TextSegment> segments = textSplitter.split(content);
        
        // Convert each chunk to embedding vector (1536 dimensions)
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment.text());
            embeddingStore.add(embedding, segment);
        }
    }
}
```

So now I have a vector database with all policy rules. Each chunk of text is represented as a 1536-dimensional vector.

### Semantic Search

When the AI needs to find relevant rules, it calls the `PolicyRulesRagTool`:

```java
@Tool("Search policy rules knowledge base for relevant rules")
public String searchRules(String query) {
    // Convert query to embedding
    Embedding queryEmbedding = embeddingModel.embed(query);
    
    // Find top 5 most similar chunks (cosine similarity)
    List<EmbeddingMatch<TextSegment>> matches = 
        embeddingStore.findRelevant(queryEmbedding, 5);
    
    // Return the text of matched chunks
    return matches.stream()
        .map(match -> match.embedded().text())
        .collect(Collectors.joining("\n\n"));
}
```

**Example**:
- AI searches: "exclusions for disease death"
- This gets converted to an embedding vector
- System finds the 5 most similar chunks in the vector database
- Returns something like: "Deaths from pre-existing conditions not disclosed are excluded..."
- AI uses this information to make a decision

**Why this is better than keyword search:**
- Semantic search understands meaning, not just words
- "Is suicide covered?" and "suicide coverage rules" return the same results
- Handles synonyms and context automatically

---

## API Communication - React and Spring Boot

Let me explain how React communicates with Spring Boot in the cloud.

### The Architecture

```
React Frontend (Static Web App)
    ↓
    | HTTPS REST API Call
    ↓
Spring Boot Backend (App Service)
    ↓
    ├─→ Azure SQL Database
    ├─→ Azure Blob Storage
    ├─→ Azure OpenAI
    └─→ Google Search API
```

### React Makes the Call

In my React app, I have an API service:

```javascript
// src/services/claimService.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const submitClaim = async (formData) => {
  const response = await axios.post(
    `${API_BASE_URL}/api/claim/submit`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  
  return response.data;
};
```

**In development**: API_BASE_URL = `http://localhost:8080`  
**In production**: API_BASE_URL = `https://metlife-claim-processor.azurewebsites.net`

### Spring Boot Responds

My ClaimController always returns HTTP 200, even for rejections. Why? Because I want React to be able to read the response body and show the user why the claim was rejected:

```java
@PostMapping("/api/claim/submit")
public ResponseEntity<ClaimResponse> submitClaim(@ModelAttribute ClaimRequest request) {
    ClaimResponse response = policyRuleService.evaluateClaim(request);
    return ResponseEntity.ok(response);  // Always 200 OK
}
```

The response looks like:
```json
{
  "status": "APPROVED",  // or "REJECTED" or "MANUAL_REVIEW"
  "message": "Claim approved based on verification",
  "claimReference": "CLM-2026-1234"
}
```

### CORS Configuration

Since React runs on a different domain than Spring Boot, I need to enable CORS:

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // Development
            "https://metlife-claims.azurestaticapps.net"  // Production
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        return new CorsFilter(source);
    }
}
```

This tells Spring Boot: "Accept requests from these React domains."

### Important: Who Uses Whose API?

React USES Spring Boot's API. Spring Boot does NOT call React. React is just static files (HTML, CSS, JS) that run in the browser. Spring Boot is the server that React talks to.

Spring Boot DOES call external APIs:
- Azure OpenAI API (for GPT-4o and embeddings)
- Azure Blob Storage API (for file storage)
- Azure SQL Database (for data)
- Google Search API (for verification)

---

## Docker Configuration

Let me explain how I containerized this application using Docker.

### Why Docker?

Without Docker, deploying to production is messy:
- "It works on my laptop but not on the server"
- Different Java versions
- Missing dependencies
- Configuration headaches

With Docker:
- Package everything (code, Java runtime, dependencies) in one container
- Same container works on my Mac, Windows, Linux, Azure, AWS, anywhere
- Consistent behavior everywhere

### The Dockerfile

I used a multi-stage build to keep the image small:

```dockerfile
# STAGE 1: Build the JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Security: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why two stages?**

Stage 1 (Build):
- Uses Maven to compile Java code
- Creates JAR file
- Image size: ~1.2 GB (includes Maven, JDK, build tools)

Stage 2 (Runtime):
- Takes only the JAR from stage 1
- Uses lightweight JRE (not full JDK)
- Image size: ~200 MB

Final image: Only 200MB instead of 1.2GB!

### Building the Docker Image

```bash
docker build -t metlife-claim-processor:latest .
```

This reads the Dockerfile and creates an image.

### Running Locally with Docker Compose

I created a `docker-compose.yml` to make it easy to run:

```yaml
services:
  metlife-claim-processor:
    image: metlife-claim-processor:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlserver://metlife.database.windows.net...
      - AZURE_OPENAI_KEY=${AZURE_OPENAI_KEY}
      - AZURE_STORAGE_CONNECTION_STRING=${AZURE_STORAGE_CONNECTION_STRING}
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
```

Then just run:
```bash
docker-compose up -d
```

And the entire application starts with all configurations!

---

## Deploying to Azure Cloud

Finally, let me explain how I deployed this to Azure.

### Why Azure App Service?

I chose Azure App Service because:
- It's a Platform as a Service (PaaS) - I don't manage servers
- Auto-scaling built-in
- Easy deployment (just upload JAR or Docker image)
- Integrates seamlessly with other Azure services
- Built-in HTTPS and custom domains

### Deployment Steps with Azure CLI

I automated the entire deployment with a shell script `deploy-azure.sh`:

**Step 1: Create Resource Group**
```bash
az group create --name metlife-rg --location eastus2
```

Resource group is like a folder that holds all related Azure resources.

**Step 2: Create App Service Plan**
```bash
az appservice plan create \
  --name metlife-plan \
  --resource-group metlife-rg \
  --sku B2 \
  --is-linux
```

App Service Plan defines the compute resources (CPU, RAM). B2 = 2 cores, 3.5GB RAM.

**Step 3: Create Web App**
```bash
az webapp create \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --plan metlife-plan \
  --runtime "JAVA:17-java17"
```

This creates the actual web application running Java 17.

**Step 4: Configure Environment Variables**
```bash
az webapp config appsettings set \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --settings \
    SPRING_DATASOURCE_URL="jdbc:sqlserver://..." \
    AZURE_OPENAI_KEY="..." \
    AZURE_STORAGE_CONNECTION_STRING="..."
```

All my API keys and connection strings go here as environment variables.

**Step 5: Enable CORS**
```bash
az webapp cors add \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --allowed-origins "https://metlife-claims.azurestaticapps.net"
```

This allows React frontend to call the backend API.

**Step 6: Build and Deploy**
```bash
# Build JAR
mvn clean package -DskipTests

# Deploy to Azure
az webapp deploy \
  --name metlife-claim-processor \
  --resource-group metlife-rg \
  --src-path target/*.jar \
  --type jar
```

The JAR gets uploaded, and Azure starts the application automatically!

**Final URL**: `https://metlife-claim-processor.azurewebsites.net`

### React Frontend Deployment

React gets deployed separately to Azure Static Web Apps:

```bash
# Build React app
npm run build

# Deploy
az staticwebapp create \
  --name metlife-claims-frontend \
  --resource-group metlife-rg \
  --source "./build"
```

Then I configure React to point to the backend API using an environment variable.

### Complete Cloud Architecture

```
Internet Users
    ↓
Azure Static Web Apps (React Frontend)
URL: https://metlife-claims.azurestaticapps.net
    ↓
    | HTTPS API Calls
    ↓
Azure App Service (Spring Boot Backend)
URL: https://metlife-claim-processor.azurewebsites.net
    ↓
    ├─→ Azure SQL Database (Policy & Claim data)
    ├─→ Azure Blob Storage (Document files)
    ├─→ Azure OpenAI (GPT-4o, Embeddings)
    └─→ Google Search API (Verification)
```

---

## Key Features Recap

Let me summarize the key features I implemented:

1. **Policy Validation First** - Check if policy exists and is active before processing anything. Saves time and resources.

2. **Multiple Rejection Handling** - If a policy has been rejected twice, the third attempt goes directly to manual review. Prevents repeated fraud attempts.

3. **Critical Pre-Validation** - Policy number and holder name from the web form MUST appear in the OCR extracted text. Catches obvious fraud early before calling expensive AI.

4. **Disease Verification** - If the filled form says death was due to a specific disease, that exact disease name must appear in hospital records. Disease mismatch = fraud.

5. **Testing Bypass** - If all documents contain "Verified by: Tejas Avhad" at the bottom, skip all validation checks. Super useful during development and testing.

6. **AI Agent with Tools** - The AI has access to three tools: database queries, policy rules search, and web search. It autonomously decides when to use each tool.

7. **RAG System** - 9 categories of policy rules stored as embeddings. AI can semantically search for relevant rules.

8. **Autonomous Web Search** - AI decides when to Google search hospitals or police stations. Not hardcoded - the AI figures it out.

9. **GPT-4o Vision OCR** - Extract text from scanned documents with high accuracy. No need for traditional OCR services.

10. **Complete Audit Trail** - Everything saved to database with timestamps, AI reasoning, document URLs.

---

## What I Learned

This project taught me a lot:

**Technical Skills:**
- Building AI agents with LangChain4j
- Implementing RAG for enterprise knowledge
- Working with Azure OpenAI GPT-4o and Vision models
- Vector embeddings and semantic search
- Docker containerization
- Azure cloud deployment
- REST API design with Spring Boot

**Problem Solving:**
- How to balance between automation and manual review
- When to fail fast (policy check first)
- How to make AI deterministic for critical decisions (temperature 0.0 for OCR, 0.3 for fraud detection)
- Handling edge cases (multiple rejections, missing documents, network failures)

**Best Practices:**
- Multi-stage Docker builds for smaller images
- CORS configuration for production
- Environment variables for sensitive data
- Error handling and graceful degradation
- Cost optimization (pre-validation before AI calls)

---

## Future Improvements

If I had more time, I would add:

1. **Authentication** - JWT tokens for secure API access
2. **Rate Limiting** - Prevent abuse of API endpoints
3. **Caching** - Cache OCR results for same documents
4. **Batch Processing** - Process multiple claims simultaneously
5. **Admin Dashboard** - View statistics, manual review queue
6. **Notification System** - Email/SMS when claim is processed
7. **More RAG Categories** - Add more policy rule types
8. **Multi-language Support** - Process documents in different languages
9. **Advanced Fraud Patterns** - ML model for fraud probability scoring
10. **API Documentation** - Swagger/OpenAPI documentation

---

## Conclusion

So yeah, that's my MetLife Claim Processor project! It's an end-to-end AI-powered system that:
- Automates claim verification (minutes instead of hours)
- Detects fraud with high accuracy
- Reduces human error
- Scales easily in the cloud
- Handles edge cases intelligently

The coolest part? The AI agent that autonomously uses tools to verify information. It's not just a chatbot - it's a smart assistant that can query databases, search policy rules, and even Google search to verify claims.

I'm really proud of how it turned out, and I learned a ton building it!

Any questions? I'd be happy to explain any part in more detail!
