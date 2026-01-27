# Multi-Document OCR Verification - How It Works

## Overview
The system processes **ALL document types** and performs OCR on each one. The AI agent then analyzes the combined text from all documents to detect fraud.

---

## Document Types Processed

### 1. **Claim Form** (Required)
- **Always processed**: Yes
- **OCR extracted**: Yes
- **Label**: `=== CLAIM FORM DOCUMENT ===`
- **Contains**: Policyholder info, claimant details, cause of death

### 2. **Death Certificate** (Optional)
- **Processed if uploaded**: Yes
- **OCR extracted**: Yes
- **Label**: `=== DEATH CERTIFICATE DOCUMENT ===`
- **Contains**: Official death record, date, cause, doctor signature

### 3. **Doctor/Hospital Report** (Optional)
- **Processed if uploaded**: Yes
- **OCR extracted**: Yes
- **Label**: `=== DOCTOR/HOSPITAL REPORT DOCUMENT ===`
- **Contains**: Medical details, hospital name, treatment, diagnosis

### 4. **Police Report** (Optional)
- **Processed if uploaded**: Yes
- **OCR extracted**: Yes
- **Label**: `=== POLICE REPORT DOCUMENT ===`
- **Contains**: Accident details, police station name, investigation

---

## How Multi-Document Verification Works

### Step 1: OCR Extraction (All Documents)
```java
// Process Claim Form
String claimFormText = documentAIService.extractTextFromImage(claimForm);

// Process Death Certificate (if provided)
String deathCertText = documentAIService.extractTextFromImage(deathCertificate);

// Process Doctor Report (if provided)
String doctorReportText = documentAIService.extractTextFromImage(doctorReport);

// Process Police Report (if provided)
String policeReportText = documentAIService.extractTextFromImage(policeReport);
```

### Step 2: Combine with Clear Labels
```
=== CLAIM FORM DOCUMENT ===
[OCR text from claim form]

=== DEATH CERTIFICATE DOCUMENT ===
[OCR text from death certificate]

=== DOCTOR/HOSPITAL REPORT DOCUMENT ===
[OCR text from doctor report]

=== POLICE REPORT DOCUMENT ===
[OCR text from police report]
```

### Step 3: AI Agent Analysis
The AI agent receives **ALL combined text** and:

1. **Cross-verifies information** across documents:
   - Names match?
   - Dates consistent?
   - Cause of death same?
   - Hospital/police names match?

2. **Fetches policy rules** using PolicyTool

3. **Verifies entities** using web search:
   - Hospital exists?
   - Police station real?

4. **Detects fraud**:
   - Fake/gibberish text
   - Inconsistent information
   - Missing critical documents
   - Non-existent entities

---

## Console Output Example

```
[PolicyRuleService] ========== STARTING OCR EXTRACTION ==========

[PolicyRuleService] ✓ ClaimForm OCR SUCCESS - Extracted 1234 chars
[OCR - ClaimForm] >>> Claim Application, Policy: POL123, Name: John Doe...

[PolicyRuleService] ✓ DeathCertificate OCR SUCCESS - Extracted 856 chars
[OCR - DeathCert] >>> Death Certificate, Deceased: John Doe, Date: 2026-01-15...

[PolicyRuleService] ✓ DoctorReport OCR SUCCESS - Extracted 1502 chars
[OCR - DoctorReport] >>> Medical Report, Hospital: Apollo Mumbai, Diagnosis...

[PolicyRuleService] ✓ PoliceReport OCR SUCCESS - Extracted 923 chars
[OCR - PoliceReport] >>> FIR No. 456/2026, Station: Andheri Police, Accident...

[PolicyRuleService] ========== OCR EXTRACTION COMPLETE ==========
[PolicyRuleService] Total extracted text length: 4515 chars
[PolicyRuleService] Document types processed: Claim Form, Death Certificate, Doctor Report, Police Report

[COMBINED OCR TEXT FROM ALL DOCUMENTS]
=== CLAIM FORM DOCUMENT ===
[Full claim form text]

=== DEATH CERTIFICATE DOCUMENT ===
[Full death cert text]

=== DOCTOR/HOSPITAL REPORT DOCUMENT ===
[Full doctor report text]

=== POLICE REPORT DOCUMENT ===
[Full police report text]
[END OCR TEXT]

========== STARTING AI AGENT ANALYSIS ==========
[ClaimAiAgentService] Calling AI Agent with PolicyTool...
[ClaimAiAgentService] AI Agent Response:
{"decision":"MANUAL_REVIEW", "reason":"Death date mismatch between certificate (2026-01-15) and police report (2026-01-20). Hospital name 'Apollo Mumbai' verified via web search. Requires manual verification of date discrepancy."}
========== AI AGENT ANALYSIS COMPLETE ==========
```

---

## Cross-Document Verification Examples

### Example 1: Date Mismatch Detection
```
Claim Form: Death date: 2026-01-15
Death Certificate: Death date: 2026-01-20
```
**AI Decision**: `MANUAL_REVIEW` - "Date inconsistency detected"

### Example 2: Name Verification
```
Claim Form: Deceased: John Smith
Death Certificate: Deceased: John Doe
```
**AI Decision**: `REJECTED` - "Name mismatch across documents"

### Example 3: Hospital Verification
```
Doctor Report: Hospital: Apollo Mumbai
Web Search: ✓ Apollo Hospital Mumbai exists
```
**AI Decision**: Continues verification

### Example 4: Fake Hospital Detection
```
Doctor Report: Hospital: XYZ123 Random Hospital
Web Search: ✗ No results found
```
**AI Decision**: `REJECTED` - "Hospital not found, suspected fraud"

### Example 5: Missing Required Document
```
Cause of Death: Accident
Documents: Claim Form, Death Certificate
Missing: Police Report
```
**AI Decision**: `MANUAL_REVIEW` - "Police report required for accidental death claims"

---

## Two Processing Methods

### Method 1: `evaluatePureAI()` - Single Document
```java
// Only processes claim form
String extractedText = documentAIService.extractTextFromImage(claimForm);
```
**Use case**: Quick claim with only claim form

### Method 2: `evaluate()` - All Documents (Recommended)
```java
// Processes ALL documents
StringBuilder allText = new StringBuilder();
allText.append("=== CLAIM FORM DOCUMENT ===\n" + claimFormText);
allText.append("\n\n=== DEATH CERTIFICATE DOCUMENT ===\n" + deathCertText);
allText.append("\n\n=== DOCTOR/HOSPITAL REPORT DOCUMENT ===\n" + doctorReportText);
allText.append("\n\n=== POLICE REPORT DOCUMENT ===\n" + policeReportText);
```
**Use case**: Comprehensive fraud detection with full documentation

---

## AI Agent Intelligence

The LangChain4j AI Agent is smart enough to:

✅ **Understand document structure** - Knows which text came from which document
✅ **Cross-reference information** - Compares data across documents
✅ **Use tools intelligently** - Calls PolicyTool and WebSearchTool when needed
✅ **Detect patterns** - Identifies fraud indicators
✅ **Make context-aware decisions** - Considers all documents holistically

---

## Fraud Detection Capabilities

### Document-Level Fraud
- Fake/AI-generated images (gibberish OCR)
- Meaningless text
- Wrong document type uploaded

### Cross-Document Fraud
- Inconsistent names/dates
- Conflicting cause of death
- Missing required documents

### Entity Verification Fraud
- Non-existent hospitals
- Fake police stations
- Invalid addresses

### Policy Violation Fraud
- Claim outside coverage
- Suicide within exclusion period
- Pre-existing condition claims

---

## Summary

**Yes, the system verifies ALL documents**, not just the claim form:
1. ✅ Extracts OCR from each document
2. ✅ Labels each document type clearly
3. ✅ Combines all text
4. ✅ Passes to AI agent
5. ✅ AI cross-verifies information
6. ✅ Detects fraud across all documents
7. ✅ Makes informed decision

**The full document verification is in the `evaluate()` method, which is the recommended approach for production use.**
