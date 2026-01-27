# 🤖 AI Services Explanation: Deep Dive

## Overview

Your MetLife Claim Processor uses **two distinct AI services** working together for intelligent claim processing. Here's how they differ and work together:

```
┌─────────────────────────────────────────────────────────────────┐
│                     AI SERVICES ARCHITECTURE                     │
│                                                                  │
│  ┌───────────────────────────────────────────────────────┐     │
│  │  1️⃣  PolicyRagService (Knowledge Base)                │     │
│  │      - Vector database for policy rules               │     │
│  │      - Embeddings + semantic search                   │     │
│  │      - Retrieves relevant rules                       │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                  │
│  ┌───────────────────────────────────────────────────────┐     │
│  │  2️⃣  ClaimAiAgentService (AI Agent with Tools)        │     │
│  │      - LangChain4j AI Agent                           │     │
│  │      - Uses PolicyTool (database queries)             │     │
│  │      - Uses PolicyRulesRagTool (RAG retrieval)        │     │
│  │      - Optional: Google Search                        │     │
│  └───────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1️⃣ PolicyRagService (Knowledge Base)

### 📍 Location
[PolicyRagService.java](src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRagService.java)

### 🎯 Purpose
**RAG (Retrieval Augmented Generation)** - Store policy rules in vector database for semantic search and retrieval.

### 🔧 How It Works

#### **Step 1: Load Policy Rules at Startup**

```java
@PostConstruct
public void init() {
    // 1. Initialize Azure OpenAI Embedding Model
    embeddingModel = AzureOpenAiEmbeddingModel.builder()
        .endpoint(azureOpenAiEndpoint)
        .apiKey(azureOpenAiKey)
        .deploymentName("text-embedding-ada-002")
        .build();
    
    // 2. Create in-memory vector store
    embeddingStore = new InMemoryEmbeddingStore<>();
    
    // 3. Load default policy rules
    loadDefaultPolicyRules();
}

private void loadDefaultPolicyRules() {
    List<String> policyRules = List.of(
        "MetLife Suicide Coverage: NOT covered within first year...",
        "MetLife Accidental Death: Police FIR mandatory...",
        "MetLife Document Requirements: Death certificate mandatory...",
        // ... 9 different policy rule categories
    );
    
    // Convert text to vector embeddings and store
    for (String rule : policyRules) {
        Document doc = Document.from(rule);
        List<TextSegment> segments = splitter.split(doc);
        
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);  // Store in vector DB
        }
    }
}
```

#### **Step 2: Retrieve Relevant Rules**

```java
public String retrieveRelevantPolicyRules(String query, int maxResults) {
    // 1. Convert query to embedding
    Embedding queryEmbedding = embeddingModel.embed(query).content();
    
    // 2. Find most similar rules in vector store
    List<EmbeddingMatch<TextSegment>> matches = 
        embeddingStore.findRelevant(queryEmbedding, maxResults);
    
    // 3. Return matched rules
    return matches.stream()
        .map(match -> match.embedded().text())
        .collect(Collectors.joining("\n\n"));
}
```

### 📊 Architecture

### 📍 Location
[PolicyRagService.java](src/main/java/com/tejas/metlife/claimprocessor/service/PolicyRagService.java)

### 🎯 Purpose
**RAG (Retrieval Augmented Generation)** - Store policy rules in vector database for semantic search and retrieval.

### 🔧 How It Works

#### **Step 1: Load Policy Rules at Startup**

```java
@PostConstruct
public void init() {
    // 1. Initialize Azure OpenAI Embedding Model
    embeddingModel = AzureOpenAiEmbeddingModel.builder()
        .endpoint(azureOpenAiEndpoint)
        .apiKey(azureOpenAiKey)
        .deploymentName("text-embedding-ada-002")
        .build();
    
    // 2. Create in-memory vector store
    embeddingStore = new InMemoryEmbeddingStore<>();
    
    // 3. Load default policy rules
    loadDefaultPolicyRules();
}

private void loadDefaultPolicyRules() {
    List<String> policyRules = List.of(
        "MetLife Suicide Coverage: NOT covered within first year...",
        "MetLife Accidental Death: Police FIR mandatory...",
        "MetLife Document Requirements: Death certificate mandatory...",
        // ... 9 different policy rule categories
    );
    
    // Convert text to vector embeddings and store
    for (String rule : policyRules) {
        Document doc = Document.from(rule);
        List<TextSegment> segments = splitter.split(doc);
        
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);  // Store in vector DB
        }
    }
}
```

#### **Step 2: Retrieve Relevant Rules**

```java
public String retrieveRelevantPolicyRules(String query, int maxResults) {
    // 1. Convert query to embedding
    Embedding queryEmbedding = embeddingModel.embed(query).content();
    
    // 2. Find most similar rules in vector store
    List<EmbeddingMatch<TextSegment>> matches = 
        embeddingStore.findRelevant(queryEmbedding, maxResults);
    
    // 3. Return matched rules
    return matches.stream()
        .map(match -> match.embedded().text())
        .collect(Collectors.joining("\n\n"));
}
```

### 📊 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   POLICY RAG SERVICE                         │
│                                                              │
│  Startup Phase:                                             │
│  ┌──────────────────────────────────────────────────┐      │
│  │ 1. Load 9 policy rule categories                 │      │
│  │    - Suicide coverage rules                      │      │
│  │    - Accidental death rules                      │      │
│  │    - Document requirements                       │      │
│  │    - Exclusions                                  │      │
│  │    - Fraud detection guidelines                  │      │
│  │    - etc...                                      │      │
│  └──────────────────────────────────────────────────┘      │
│                      ↓                                       │
│  ┌──────────────────────────────────────────────────┐      │
│  │ 2. Convert to embeddings (vector numbers)        │      │
│  │    "Suicide NOT covered in first year"           │      │
│  │    → [0.234, -0.567, 0.891, ... 1536 numbers]   │      │
│  └──────────────────────────────────────────────────┘      │
│                      ↓                                       │
│  ┌──────────────────────────────────────────────────┐      │
│  │ 3. Store in InMemoryEmbeddingStore (Vector DB)   │      │
│  │    ┌─────────────────────────────────────┐       │      │
│  │    │ Segment 1: [vec1]                   │       │      │
│  │    │ Segment 2: [vec2]                   │       │      │
│  │    │ Segment 3: [vec3]                   │       │      │
│  │    │ ... ~50 segments total              │       │      │
│  │    └─────────────────────────────────────┘       │      │
│  └──────────────────────────────────────────────────┘      │
│                                                              │
│  Query Phase:                                               │
│  ┌──────────────────────────────────────────────────┐      │
│  │ Query: "suicide claim rules"                     │      │
│  │    ↓ Convert to embedding                        │      │
│  │ [0.241, -0.559, 0.888, ...]                     │      │
│  │    ↓ Find similar vectors                        │      │
│  │ Returns: "Suicide NOT covered within first year, │      │
│  │          Police report required..."              │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 🧠 What Are Embeddings?

Think of embeddings as **meaning coordinates**:

```
Text: "Suicide coverage rules"
   ↓ Azure OpenAI Embedding Model
Vector: [0.234, -0.567, 0.891, 0.123, ... 1536 numbers]

Similar text has similar vectors:
"Suicide claim guidelines" → [0.239, -0.561, 0.885, 0.118, ...]
"Accidental death rules"   → [0.892, 0.334, -0.221, 0.667, ...]
                              ↑ Very different numbers!
```

### 📚 Policy Rules Stored

1. **General Policy Rules**: Active policy, premium paid, filing deadlines
2. **Suicide Coverage**: 1-year exclusion period
3. **Accidental Death**: Police FIR, hospital records
4. **Natural Death**: Medical certificates
5. **Disease Coverage**: Treatment records
6. **Document Requirements**: Mandatory documents list
7. **Exclusions**: War, terrorism, illegal activities
8. **Claim Timeline**: Processing deadlines
9. **Fraud Detection**: Red flags to watch

### ✅ Pros
- **Semantic search**: Finds relevant rules even if exact keywords don't match
- **Scalable**: Can add more rules dynamically
- **Fast retrieval**: Vector similarity search is quick
- **Context-aware**: Provides relevant policy context to AI

### ❌ Cons
- **Requires embedding model**: Needs Azure OpenAI text-embedding-ada-002
- **In-memory only**: Lost on restart (not persistent database)
- **Static rules**: Rules hardcoded in Java (not database-driven)

### 🔍 Use Case
**Provide policy knowledge** to the AI agent so it makes decisions based on actual MetLife rules.

---

## 2️⃣ ClaimAiAgentService (AI Agent with Tools)

### 📍 Location
[ClaimAiAgentService.java](src/main/java/com/tejas/metlife/claimprocessor/service/ClaimAiAgentService.java)

### 🎯 Purpose
**Intelligent AI Agent** that can use tools (database queries, RAG retrieval, web search) to make informed decisions.

### 🔧 How It Works

#### **Step 1: Initialize AI Agent with Tools**

```java
@PostConstruct
public void init() {
    // 1. Create Azure OpenAI Chat Model
    ChatLanguageModel chatModel = AzureOpenAiChatModel.builder()
        .endpoint(azureOpenAiEndpoint)
        .apiKey(azureOpenAiKey)
        .deploymentName(azureOpenAiDeployment)  // gpt-4o
        .temperature(0.7)
        .build();
    
    // 2. Build AI Agent with TOOLS
    claimAgent = AiServices.builder(ClaimAgent.class)
        .chatLanguageModel(chatModel)
        .tools(
            policyTool,           // Database queries
            policyRulesRagTool,   // RAG retrieval
            webSearchEngine       // Google Search (optional)
        )
        .build();
    
    System.out.println("✓ AI Agent with 3 tools ready!");
}
```

#### **Step 2: Agent Analyzes Claim Using Tools**

```java
public AiDecision analyzeClaim(String extractedText, String policyNumber) {
    // Call the AI agent
    String jsonResponse = claimAgent.analyze(extractedText, policyNumber);
    
    // Agent will:
    // 1. Read the prompt
    // 2. Decide which tools to call
    // 3. Call PolicyTool to get policy from database
    // 4. Call PolicyRulesRagTool to get relevant rules
    // 5. Optionally call web search for verification
    // 6. Combine all information
    // 7. Make final decision
    
    return parseResponse(jsonResponse);
}
```

### 📊 Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                  CLAIM AI AGENT SERVICE                         │
│                                                                 │
│  ┌──────────────────────────────────────────────────────┐     │
│  │  ClaimAgent (LangChain4j AI Agent Interface)         │     │
│  │  ------------------------------------------------     │     │
│  │  String analyze(String extractedText,                │     │
│  │                 String policyNumber);                │     │
│  └──────────────────────────────────────────────────────┘     │
│                          ↓                                      │
│  ┌──────────────────────────────────────────────────────┐     │
│  │  Azure OpenAI GPT-4o (with tool calling)             │     │
│  │  - Receives prompt                                    │     │
│  │  - Decides which tools to call                        │     │
│  │  - Executes tools autonomously                        │     │
│  │  - Combines results                                   │     │
│  │  - Makes final decision                               │     │
│  └──────────────────────────────────────────────────────┘     │
│             ↓                ↓                ↓                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  PolicyTool   │  │PolicyRulesRag│  │ WebSearchTool│        │
│  │              │  │     Tool     │  │   (optional)  │        │
│  │  Fetches     │  │              │  │              │        │
│  │  policy from │  │  Retrieves   │  │  Google      │        │
│  │  database:   │  │  relevant    │  │  search for  │        │
│  │              │  │  policy rules│  │  verification│        │
│  │  POL123456   │  │  from RAG    │  │              │        │
│  │  → Policy    │  │  vector DB   │  │  "hospital   │        │
│  │    object    │  │              │  │   name India"│        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└────────────────────────────────────────────────────────────────┘
```

### 🛠️ Tools Available to Agent

#### **Tool 1: PolicyTool** (Database Query)
```java
@Tool("Fetch specific policy details from database using policy number")
public String fetchPolicyDetails(String policyNumber) {
    Optional<Policy> policy = policyRepository.findByPolicyNumber(policyNumber);
    
    if (policy.isEmpty()) {
        return "Policy not found";
    }
    
    Policy p = policy.get();
    return String.format("""
        Policy Number: %s
        Holder Name: %s
        Sum Assured: ₹%,.2f
        Status: %s
        Premium Paid: %s
        Start Date: %s
        Nominee: %s
        """, 
        p.getPolicyNumber(),
        p.getHolderName(),
        p.getSumAssured(),
        p.getPolicyStatus(),
        p.getPremiumPaid() ? "Yes" : "No",
        p.getStartDate(),
        p.getNomineeName()
    );
}
```

**What it does**: Queries Azure SQL Database for policy details.

---

#### **Tool 2: PolicyRulesRagTool** (RAG Retrieval)
```java
@Tool("Retrieve relevant MetLife policy rules and guidelines using RAG")
public String retrievePolicyRules(String query) {
    return policyRagService.retrieveRelevantPolicyRules(query, 5);
}
```

**What it does**: Asks PolicyRagService for relevant rules from vector database.

---

#### **Tool 3: WebSearchTool** (Google Search - Optional)
```java
// Configured with Google Custom Search API
WebSearchEngine webSearchEngine = GoogleCustomWebSearchEngine.builder()
    .apiKey(googleSearchApiKey)
    .csi(googleSearchEngineId)
    .build();
```

**What it does**: Searches Google to verify hospitals, locations, or other facts.

---

### 🎬 Example: How the Agent Works

**User submits claim**: Policy POL123456, Death by accident

**Agent's internal thought process** (automatically):

```
1. Agent reads prompt: "Analyze this claim..."

2. Agent thinks: "I need policy details first"
   → Calls PolicyTool.fetchPolicyDetails("POL123456")
   → Receives: "Policy active, sum assured ₹10L, holder: John Doe"

3. Agent thinks: "Need accidental death rules"
   → Calls PolicyRulesRagTool.retrievePolicyRules("accidental death coverage")
   → Receives: "Police FIR mandatory, death within 180 days of accident..."

4. Agent thinks: "Let me verify the hospital mentioned"
   → Calls WebSearchTool("City Hospital Delhi India")
   → Receives: "City Hospital is a registered hospital in Delhi"

5. Agent combines all information:
   - Policy is valid ✓
   - Rules say FIR needed - OCR text shows FIR number ✓
   - Hospital exists ✓
   - Claim amount ₹5L < Sum assured ₹10L ✓

6. Agent decides: "APPROVED"
   
7. Returns JSON:
   {
     "decision": "APPROVED",
     "reason": "Policy active, all documents valid, FIR present, 
                hospital verified, claim within limits"
   }
```

### 🔄 Execution Flow

```
User Request
    ↓
ClaimAiAgentService.analyzeClaim(text, policyNumber)
    ↓
┌─────────────────────────────────────────┐
│ AI Agent (GPT-4o with tool calling)    │
│                                         │
│ [AI reads prompt and decides...]        │
│                                         │
│ "I need to call fetchPolicyDetails"    │  ← Agent decides autonomously
│     ↓ Calls PolicyTool                 │
│ "Policy found: John Doe, ₹10L"         │
│                                         │
│ "Now I need accidental death rules"    │  ← Agent decides next step
│     ↓ Calls PolicyRulesRagTool         │
│ "Rules: FIR required, 180 days limit"  │
│                                         │
│ "Let me verify hospital name"          │  ← Agent decides verification needed
│     ↓ Calls WebSearchTool              │
│ "Hospital exists in Delhi"             │
│                                         │
│ "I have all info, making decision..."  │  ← Agent synthesizes
│     ↓ Returns decision                 │
│ {"decision":"APPROVED","reason":"..."}  │
│                                         │
└─────────────────────────────────────────┘
    ↓
Parse JSON response
    ↓
Return AiDecision to controller
```

### ✅ Pros
- **Intelligent**: AI decides which tools to use
- **Comprehensive**: Access to database, policy rules, and web
- **Flexible**: Can add more tools easily
- **Accurate**: Fact-checks against real data
- **Autonomous**: No manual orchestration needed

### ❌ Cons
- **Complex**: More moving parts
- **Slower**: Multiple tool calls take time
- **Expensive**: More API calls = higher cost
- **Requires LangChain4j**: Additional dependency

### 🔍 Use Case
**Production claim processing** - Full analysis with database verification, policy rule checking, and optional web verification.

---

## 🔄 How They Work Together

### Scenario: Processing a Claim

```
┌─────────────────────────────────────────────────────────────────┐
│  Claim Processing with AI Agent                                 │
│  ────────────────────────────────────────────────────────────   │
│  ClaimAiAgentService.analyzeClaim(extractedText, policyNumber) │
│                                                                  │
│  Agent internally calls:                                        │
│  1. PolicyTool → fetch policy from database                    │
│  2. PolicyRulesRagTool → get relevant rules                    │
│      (which uses PolicyRagService.retrieveRelevantPolicyRules) │
│  3. WebSearchTool (optional) → verify facts                    │
│                                                                  │
│  Combines all information and makes final decision              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
                   Final Decision
```

---

## 📊 Comparison Table

| Feature | PolicyRagService | ClaimAiAgentService |
|---------|------------------|---------------------|
| **Purpose** | Policy knowledge base | Intelligent agent |
| **Complexity** | Medium | Complex |
| **API** | Azure OpenAI embeddings | LangChain4j + OpenAI |
| **Tools** | N/A (is a tool itself) | 3 tools |
| **Database Access** | ❌ No | ✅ Yes (via PolicyTool) |
| **Policy Rules** | ✅ Yes (RAG) | ✅ Yes (via RAG tool) |
| **Web Search** | ❌ No | ✅ Optional |
| **Decision Making** | N/A | Advanced |
| **Speed** | Fast | Moderate |
| **Cost** | Low | Higher |
| **Accuracy** | N/A | High |
| **Use Case** | Provide context | Claim decision making |

---

## 🎯 How They Work

### **PolicyRagService** (Knowledge Base):
- 🎯 Stores policy rules as vector embeddings
- 🔍 Enables semantic search over MetLife policies
- 🛠️ Used by ClaimAiAgentService through PolicyRulesRagTool
- 📚 Contains 9 policy rule categories
- ⚡ Fast retrieval through vector similarity search

### **ClaimAiAgentService** (Decision Maker):
- 🤖 LangChain4j AI Agent with autonomous tool calling
- 🔧 Uses PolicyTool for database queries
- 📖 Uses PolicyRulesRagTool to access PolicyRagService
- 🌐 Optionally uses WebSearchTool for verification
- 🎯 Makes comprehensive, informed claim decisions
- ✅ Single-pass analysis with all available context

---

## 💡 Architecture Pattern: AI Agent with RAG

```java
// Single comprehensive analysis
AiDecision decision = claimAiAgentService.analyzeClaim(
    extractedText, 
    policyNumber
);

// Agent internally:
// 1. Fetches policy from database (PolicyTool)
// 2. Retrieves relevant rules from RAG (PolicyRulesRagTool → PolicyRagService)
// 3. Optionally verifies facts via web search
// 4. Combines all information
// 5. Makes informed decision

return decision;  // Comprehensive, accurate result
```

**Why this works**:
1. **Single AI analysis** with complete context
2. **Fact-checked** against real database data
3. **Policy-compliant** using RAG knowledge base
4. **Verifiable** through optional web search
5. **Best accuracy** with all available information

---

## 🔧 Configuration

### application.properties

```properties
# Azure OpenAI for AI Agent
azure.openai.endpoint=https://your-resource.openai.azure.com/
azure.openai.key=your-api-key
azure.openai.deployment=gpt-4o

# Azure OpenAI Embeddings for RAG
azure.openai.embedding.endpoint=https://your-resource.openai.azure.com/
azure.openai.embedding.key=your-api-key
azure.openai.embedding.deployment=text-embedding-ada-002

# Google Search (optional - for AI agent web verification)
google.search.api.key=your-google-api-key
google.search.engine.id=your-search-engine-id
```

---

## 📝 Summary

### PolicyRagService
**"The Knowledge Base"** - Stores policy rules as vector embeddings. Enables semantic search over MetLife policies. Provides context to the AI agent.

### ClaimAiAgentService
**"The Smart Agent"** - LangChain4j-powered agent that autonomously uses tools (database, RAG, web) to make informed decisions.

**Together**: They create an intelligent, single-pass claim processing system with comprehensive analysis and accurate decisions! 🚀
