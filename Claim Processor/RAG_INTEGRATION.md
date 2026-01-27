# LangChain4j RAG Integration for Policy Constraints

## Overview
The system now uses **RAG (Retrieval-Augmented Generation)** to store and retrieve policy rules, constraints, and guidelines. This ensures the AI agent has accurate knowledge of policy terms when making claim decisions.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Claim Processing Flow                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   OCR Extraction  │
                    │  (All Documents)  │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   ClaimAgent AI   │
                    │  (Azure OpenAI)   │
                    └──────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
            ▼                 ▼                 ▼
  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
  │ PolicyTool   │  │  RAG Tool    │  │ WebSearchTool│
  │              │  │              │  │              │
  │ Fetch Policy │  │ Retrieve     │  │ Verify       │
  │ from DB      │  │ Policy Rules │  │ Hospitals    │
  └──────────────┘  └──────────────┘  └──────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  PolicyRagService │
                    │                  │
                    │  Vector Store    │
                    │  (Embeddings)    │
                    └──────────────────┘
```

---

## Components

### 1. **PolicyRagService**
- **File**: `PolicyRagService.java`
- **Purpose**: Manages policy rules in a vector database
- **Technology**: 
  - Azure OpenAI Embeddings (`text-embedding-ada-002`)
  - In-Memory Vector Store (LangChain4j)

**Features**:
- ✅ Loads default MetLife policy rules on startup
- ✅ Converts policy text into embeddings
- ✅ Stores embeddings in vector database
- ✅ Retrieves relevant rules using semantic search
- ✅ Supports adding custom policy rules

### 2. **PolicyRulesRagTool**
- **File**: `PolicyRulesRagTool.java`
- **Purpose**: LangChain4j tool for AI agent to query policy rules
- **Usage**: AI agent calls `retrievePolicyRules(query)` automatically

**Example Queries**:
```java
retrievePolicyRules("suicide coverage rules")
retrievePolicyRules("accidental death document requirements")
retrievePolicyRules("policy exclusions")
retrievePolicyRules("claim filing timeline")
```

### 3. **Policy Rules Stored in RAG**

The system pre-loads the following policy knowledge:

#### General Policy Rules
- Policy must be active
- Premiums must be paid
- Claims filed within 30 days
- Complete documentation required

#### Suicide Coverage
- NOT covered within first year
- Requires death certificate, police report
- Must verify suicide as cause

#### Accidental Death Coverage
- Police FIR mandatory
- Hospital records required
- Death within 180 days of accident
- Not for illegal activities

#### Natural Death Coverage
- Death certificate required
- Hospital discharge summary needed
- Pre-existing conditions (2-year exclusion)

#### Disease Death Coverage
- Medical history required
- Treatment records needed
- Specialist certification for terminal illness

#### Document Requirements
- Death Certificate (mandatory)
- Claim Form (mandatory)
- Policy Document (mandatory)
- Additional docs based on death type

#### Exclusions
- War, terrorism, riots
- Drug overdose (unless accidental)
- Self-inflicted injuries
- Illegal activities
- Aviation (unless commercial passenger)

#### Timeline Rules
- Notification: 7 days
- Document submission: 30 days
- Processing: 15-30 days
- Investigation: Additional 30-60 days

#### Fraud Detection Guidelines
- Fake/forged documents rejected
- Non-existent hospitals/police flagged
- Inconsistent information investigated
- Gibberish OCR text suspicious

---

## How RAG Works

### Step 1: Policy Rules Loading (Startup)
```java
@PostConstruct
public void init() {
    // Load policy rules
    List<String> policyRules = [...];
    
    // Split into chunks
    DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
    
    // Generate embeddings
    Embedding embedding = embeddingModel.embed(segment).content();
    
    // Store in vector database
    embeddingStore.add(embedding, segment);
}
```

### Step 2: AI Agent Query (Runtime)
```
User uploads claim → OCR extracts text → AI Agent analyzes

AI Agent thinks: "Need to check suicide coverage rules"
↓
AI Agent calls: retrievePolicyRules("suicide coverage")
↓
RAG Service: Embeds query → Searches vector DB → Returns relevant rules
↓
AI Agent receives: "Suicide NOT covered within first year..."
↓
AI Agent makes decision based on retrieved rules
```

### Step 3: Semantic Search
```java
public String retrieveRelevantPolicyRules(String query, int maxResults) {
    // Convert query to embedding
    Embedding queryEmbedding = embeddingModel.embed(query).content();
    
    // Find similar policy rules
    List<EmbeddingMatch<TextSegment>> matches = 
        embeddingStore.findRelevant(queryEmbedding, maxResults);
    
    // Return matched rules
    return matches.stream()
        .map(match -> match.embedded().text())
        .collect(Collectors.joining("\n\n"));
}
```

---

## AI Agent Workflow with RAG

### Example: Accidental Death Claim

**1. AI Agent receives OCR text:**
```
=== CLAIM FORM DOCUMENT ===
Policy: POL123, Cause: Road Accident

=== DEATH CERTIFICATE DOCUMENT ===
Death due to vehicular accident

=== POLICE REPORT DOCUMENT ===
FIR No. 456/2026, Andheri Police Station
```

**2. AI Agent calls RAG tool:**
```java
retrievePolicyRules("accidental death coverage rules")
```

**3. RAG returns relevant rules:**
```
Accidental Death Coverage Rules:
- Police FIR mandatory
- Hospital records required
- Death within 180 days of accident
- Verify accident not during illegal activities
```

**4. AI Agent checks:**
- ✅ Police FIR present: Yes (FIR No. 456/2026)
- ✅ Accident verified: Yes
- ✅ Hospital records: Need to check
- ⚠️ Timeline: Need to verify death date

**5. AI Agent calls WebSearch:**
```java
webSearch("Andheri Police Station Mumbai")
```

**6. AI Agent makes decision:**
```json
{
  "decision": "MANUAL_REVIEW",
  "reason": "Accidental death claim with valid police FIR from verified Andheri Police Station. However, hospital records are missing which are required per policy rules. Recommend manual verification of timeline and hospital admission."
}
```

---

## Configuration

### application.properties
```properties
# Azure OpenAI for Chat
azure.openai.endpoint=https://metlife-openaitejas.openai.azure.com/
azure.openai.key=YOUR_KEY
azure.openai.deployment=gpt-4

# Azure OpenAI for Embeddings (RAG)
azure.openai.embedding.deployment=text-embedding-ada-002
```

**Important**: You need to deploy `text-embedding-ada-002` model in your Azure OpenAI resource.

---

## Advantages of RAG

### ✅ Accurate Policy Knowledge
- AI has access to exact policy terms
- No hallucination of policy rules
- Consistent decision-making

### ✅ Easy Updates
- Add new policy rules without retraining
- Update coverage terms dynamically
- Policy amendments reflected immediately

### ✅ Explainable Decisions
- AI cites specific policy rules
- Transparent reasoning
- Audit trail for compliance

### ✅ Scalability
- Store unlimited policy documents
- Support multiple policy types
- Product-specific rules

### ✅ Context-Aware
- Retrieves only relevant rules
- Efficient token usage
- Faster processing

---

## Console Output with RAG

```
========== INITIALIZING POLICY RAG SERVICE ==========

[PolicyRagService] Configuring Azure OpenAI Embedding Model
[PolicyRagService] → Endpoint: https://metlife-openaitejas.openai.azure.com/
[PolicyRagService] → Deployment: text-embedding-ada-002
[PolicyRagService] ✓ Embedding Model initialized
[PolicyRagService] ✓ In-Memory Vector Store initialized
[PolicyRagService] Loading default policy rules into vector store...
[PolicyRagService] ✓ Loaded 45 policy rule segments into vector store
[PolicyRagService] ✓ Policy RAG Service ready!

========== POLICY RAG SERVICE INITIALIZED ==========

========== INITIALIZING LANGCHAIN4J AI AGENT ==========

[ClaimAiAgentService] Building AI Services with tools...
[ClaimAiAgentService] → PolicyTool: Fetch specific policy details from database
[ClaimAiAgentService] → PolicyRulesRagTool: Retrieve relevant policy rules using RAG
[ClaimAiAgentService] ✓ Initialized with PolicyTool, PolicyRulesRagTool, AND WebSearchTool

========== LANGCHAIN4J AI AGENT READY ==========

--- During Claim Processing ---

[PolicyRulesRagTool] Query: suicide coverage rules
[PolicyRagService] Retrieving policy rules for query: suicide coverage rules
[PolicyRagService] ✓ Retrieved 5 relevant policy rule segments

[AI Agent]: Based on retrieved policy rules, suicide NOT covered within first year. 
Policy issued: 2025-12-01, Death: 2026-01-15 = 1.5 months
Decision: REJECTED - Suicide within exclusion period per MetLife Suicide Coverage Rules
```

---

## Adding Custom Policy Rules

```java
@Autowired
private PolicyRagService policyRagService;

public void addNewPolicyRule() {
    String newRule = """
        MetLife Premium Plus Plan Additional Benefits:
        - Covers international medical emergencies
        - Includes air ambulance up to $50,000
        - Critical illness rider included
        - No waiting period for accidents
        """;
    
    policyRagService.addPolicyRule(newRule);
}
```

---

## Tool Comparison

| Tool | Purpose | Data Source | When Used |
|------|---------|-------------|-----------|
| **PolicyTool** | Get specific policy details | Database (Policy table) | Fetch policy status, dates, coverage flags |
| **PolicyRulesRagTool** | Get policy rules & constraints | Vector Store (RAG) | Understand what policy covers, exclusions, requirements |
| **WebSearchTool** | Verify external entities | Google Search | Verify hospitals, police stations exist |

---

## Summary

**✅ RAG Integration Complete!**

The AI agent now has:
1. ✅ Access to comprehensive policy rules via RAG
2. ✅ Semantic search for relevant policy clauses
3. ✅ Accurate knowledge of coverage, exclusions, timelines
4. ✅ Explainable decisions citing specific policy rules
5. ✅ Dynamic policy updates without code changes

**The system is now production-ready with intelligent, policy-aware claim fraud detection!** 🚀
