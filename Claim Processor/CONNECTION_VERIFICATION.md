# LangChain4j Integration - Connection Verification

## ✅ Integration Checklist

### 1. Maven Dependencies (pom.xml)
- ✅ `langchain4j` v0.33.0
- ✅ `langchain4j-azure-open-ai` v0.33.0
- ✅ `langchain4j-web-search-engine-google-custom` v0.33.0

### 2. Azure OpenAI Configuration (application.properties)
```properties
azure.openai.endpoint=
azure.openai.key=
azure.openai.deployment=gpt-4o-mini
```
✅ **Status**: Configured

### 3. Google Custom Search Configuration (application.properties)
```properties
google.search.api.key=YOUR_API_KEY_HERE
google.search.engine.id=
```
⚠️ **Status**: Engine ID configured, API key pending

### 4. Azure Document AI (OCR) Configuration
```properties
azure.vision.endpoint=
azure.vision.key=
```
✅ **Status**: Configured

### 5. Azure Blob Storage Configuration
```properties
azure.storage.connection-string
azure.storage.container-name=
```
✅ **Status**: Configured

### 6. Azure SQL Database Configuration
```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
```
✅ **Status**: Configured

---

## 🔄 Data Flow

```
1. Document Upload
   └─> BlobStorageService (Azure Blob) ✅

2. OCR Extraction
   └─> DocumentAIService (Azure Vision) ✅
       └─> Prints extracted text to console 📋

3. Policy Lookup
   └─> PolicyTool (Azure SQL) ✅
       └─> PolicyRepository

4. Web Search (Optional)
   └─> WebSearchTool (Google Custom Search) ⚠️ (Needs API Key)
       └─> Verify hospitals/police stations

5. AI Agent Analysis
   └─> ClaimAgent (Azure OpenAI via LangChain4j) ✅
       └─> Uses PolicyTool
       └─> Uses WebSearchTool (if configured)
       └─> Returns JSON decision

6. Save Result
   └─> ClaimRepository (Azure SQL) ✅
   └─> Update Policy status ✅
```

---

## 📋 Console Logging

### Startup Logs
```
========== INITIALIZING LANGCHAIN4J AI AGENT ==========

[ClaimAiAgentService] Configuring Azure OpenAI Chat Model
[ClaimAiAgentService] → Endpoint: 
[ClaimAiAgentService] → Deployment: gpt-4o-mini
[ClaimAiAgentService] → API Key: ✓ Configured
[ClaimAiAgentService] ✓ Azure OpenAI Chat Model configured successfully
[ClaimAiAgentService] Building AI Services with tools...
[ClaimAiAgentService] → Google Search API Key: ✓ Configured
[ClaimAiAgentService] → Search Engine ID: 
[ClaimAiAgentService] ✓ Initialized with PolicyTool AND WebSearchTool
[ClaimAiAgentService] ✓ ClaimAgent successfully initialized and ready!

========== LANGCHAIN4J AI AGENT READY ==========
```

### OCR Processing Logs
```
[PolicyRuleService] ========== STARTING OCR EXTRACTION ==========

[PolicyRuleService] Uploading claimForm to Azure Blob Storage...
[PolicyRuleService] ClaimForm uploaded: 
[PolicyRuleService] Extracting text from claimForm using Azure Document AI...
[PolicyRuleService] ✓ ClaimForm OCR SUCCESS - Extracted 1234 chars
[OCR - ClaimForm] >>> [First 200 chars of extracted text]...

========== OCR EXTRACTED TEXT ==========
[Full extracted text here]
========================================

[PolicyRuleService] ========== OCR EXTRACTION COMPLETE ==========
[PolicyRuleService] Total extracted text length: 1234 chars

[COMBINED OCR TEXT]
[All combined text from all documents]
[END OCR TEXT]
```

### AI Agent Analysis Logs
```
========== STARTING AI AGENT ANALYSIS ==========

[ClaimAiAgentService] Policy Number: POL123456
[ClaimAiAgentService] Extracted Text Length: 1234 chars
[ClaimAiAgentService] Calling AI Agent with PolicyTool...
[ClaimAiAgentService] AI Agent Response:
{"decision":"APPROVED", "reason":"All documents verified..."}

========== AI AGENT ANALYSIS COMPLETE ==========

[PolicyRuleService] AI Decision: APPROVED - Reason: All documents verified...
```

---

## 🔍 What to Check in Console

### ✅ OCR Verification
Look for:
```
========== OCR EXTRACTED TEXT ==========
[Actual text extracted from your document]
========================================
```
This shows the raw text extracted from the image.

### ✅ Policy Tool Verification
The AI agent will call PolicyTool automatically. Check logs for policy details being fetched.

### ⚠️ Web Search Tool Verification
If API key is configured, you'll see web search queries in logs.
If not configured, you'll see:
```
[ClaimAiAgentService] ⚠ Initialized with PolicyTool ONLY (no web search - API key missing)
```

### ✅ AI Agent Decision
Final decision with reasoning:
```
[PolicyRuleService] AI Decision: APPROVED - Reason: ...
```

---

## 🚨 Troubleshooting

### If OCR doesn't work:
- Check Azure Vision endpoint and key
- Verify image format (JPEG, PNG)
- Check file size limits

### If AI Agent fails:
- Verify Azure OpenAI endpoint and deployment name
- Check API key validity
- Ensure deployment is `gpt-4o` or compatible model

### If Policy Tool fails:
- Verify Azure SQL connection
- Check PolicyRepository is working
- Ensure policy exists in database

### If Web Search Tool fails (optional):
- Add Google Custom Search API key
- Verify Search Engine ID: `4436b4b468f9941c8`
- Check API is enabled in Google Cloud Console

---

## 🎯 Next Steps

1. **Add Google Custom Search API Key** (optional but recommended):
   - Get API key from: https://console.cloud.google.com/apis/credentials
   - Update `google.search.api.key` in application.properties

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Test with a claim**:
   - Upload a claim document
   - Check console for OCR extraction
   - Verify AI agent decision

4. **Monitor logs**:
   - OCR extraction details
   - Policy fetching
   - Web searches (if configured)
   - AI decisions and reasoning

---

## ✅ All Connections Summary

| Component | Status | Configuration File |
|-----------|--------|-------------------|
| Azure OpenAI (LangChain4j) | ✅ Ready | application.properties |
| Azure Document AI (OCR) | ✅ Ready | application.properties |
| Azure Blob Storage | ✅ Ready | application.properties |
| Azure SQL Database | ✅ Ready | application.properties |
| PolicyTool | ✅ Ready | ClaimAiAgentService |
| Google Custom Search | ⚠️ API Key Needed | application.properties |

**Overall Status: 🟢 READY TO TEST** (Web search optional)
