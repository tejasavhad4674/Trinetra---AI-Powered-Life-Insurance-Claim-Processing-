package com.tejas.metlife.claimprocessor.service;

import com.tejas.metlife.claimprocessor.dto.AiDecision;
import com.tejas.metlife.claimprocessor.dto.ClaimResponse;
import com.tejas.metlife.claimprocessor.model.Claim;
import com.tejas.metlife.claimprocessor.model.Policy;
import com.tejas.metlife.claimprocessor.repository.ClaimRepository;
import com.tejas.metlife.claimprocessor.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class PolicyRuleService {

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final BlobStorageService blobStorageService;
    private final DocumentAIService documentAIService;
    private final ClaimAiAgentService claimAiAgentService;

    public PolicyRuleService(PolicyRepository policyRepository,
                             ClaimRepository claimRepository,
                             BlobStorageService blobStorageService,
                             DocumentAIService documentAIService,
                             ClaimAiAgentService claimAiAgentService) {
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;
        this.blobStorageService = blobStorageService;
        this.documentAIService = documentAIService;
        this.claimAiAgentService = claimAiAgentService;
    }

    // =====================================================
    // =============== PURE AI CLAIM FLOW ==================
    // =====================================================

    public ClaimResponse evaluatePureAI(String policyNumber,
                                        MultipartFile claimForm,
                                        String causeOfDeath) {

        Optional<Policy> opt = policyRepository.findByPolicyNumber(policyNumber);
        if (opt.isEmpty()) {
            return new ClaimResponse("REJECTED", "Policy not found", null);
        }

        Policy policy = opt.get();

        // ---- Upload claim form ONCE ----
        System.out.println("[PolicyRuleService] Uploading claim form to Azure Blob Storage");
        String claimFormUrl = blobStorageService.uploadFile(claimForm);
        System.out.println("[PolicyRuleService] Claim form uploaded: " + claimFormUrl);

        // ---- OCR ----
        System.out.println("[PolicyRuleService] Starting OCR extraction using Azure Document AI");
        String extractedText =
                documentAIService.extractTextFromImage(claimForm);
        
        // Add document type label for AI agent
        String labeledText = "=== CLAIM FORM DOCUMENT ===\n" + extractedText;
        System.out.println("\n========== OCR EXTRACTED TEXT ==========\n" + extractedText + "\n========================================\n");

        // ---- AI Fraud Detection with LangChain4j Agent ----
        System.out.println("[PolicyRuleService] Calling LangChain4j AI Agent for fraud detection");
        AiDecision aiDecision =
                claimAiAgentService.analyzeClaim(labeledText, policyNumber);
        System.out.println("[PolicyRuleService] AI Decision: " + aiDecision.getDecision() + " - Reason: " + aiDecision.getReason());

        // ---- Save Claim ----
        String claimRef = generateRef();

        Claim claim = new Claim();
        claim.setClaimReference(claimRef);
        claim.setPolicyNumber(policyNumber);
        claim.setCauseOfDeath(causeOfDeath);
        claim.setClaimFormUrl(claimFormUrl);

        claim.setAiDecision(aiDecision.getDecision());
        claim.setAiReason(aiDecision.getReason());
        claim.setClaimStatus(aiDecision.getDecision());

        claimRepository.save(claim);

        // ---- Update Policy ----
        updatePolicyStatus(policy, aiDecision.getDecision());

        return new ClaimResponse(
                aiDecision.getDecision(),
                aiDecision.getReason(),
                claimRef
        );
    }

    // =====================================================
    // ======= FULL CLAIM FLOW (ALL DOCUMENTS + AI) ========
    // =====================================================

    public ClaimResponse evaluate(String policyNumber,
                                  String policyHolderName,
                                  String causeOfDeath,
                                  Map<String, MultipartFile> files,
                                  String deceasedFullName,
                                  String deceasedEmail,
                                  String deceasedMobile,
                                  String deceasedAddress,
                                  String nomineeFullName,
                                  String nomineeRelationship,
                                  String nomineeMobile) {

        System.out.println("[PolicyRuleService] Evaluating claim for policy: " + policyNumber);
        
        // ====== STEP 1: CHECK POLICY EXISTS FIRST (BEFORE PROCESSING FILES) ======
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            System.out.println("[PolicyRuleService] Policy number is missing");
            return new ClaimResponse("REJECTED", "Policy number is required.", null);
        }

        System.out.println("[PolicyRuleService] Checking if policy exists in database...");
        Optional<Policy> opt = policyRepository.findByPolicyNumber(policyNumber);
        if (opt.isEmpty()) {
            System.out.println("[PolicyRuleService] ⚠ Policy not found in database: " + policyNumber);
            String claimRef = generateRef();
            
            // Save rejected claim
            Claim claim = new Claim();
            claim.setClaimReference(claimRef);
            claim.setPolicyNumber(policyNumber);
            claim.setCauseOfDeath(causeOfDeath);
            claim.setDeceasedFullName(deceasedFullName);
            claim.setAiDecision("REJECTED");
            claim.setAiReason("Policy not found");
            claim.setClaimStatus("REJECTED");
            claimRepository.save(claim);
            
            return new ClaimResponse("REJECTED", "Policy not found", claimRef);
        }

        Policy policy = opt.get();
        System.out.println("[PolicyRuleService] ✓ Policy found - Status: " + policy.getStatus() + ", Holder: " + policy.getPolicyHolderName());

        // ====== CHECK FOR MULTIPLE REJECTION ATTEMPTS ======
        if ("REJECTED".equals(policy.getStatus())) {
            // Count how many times this policy has been rejected
            long rejectionCount = claimRepository.countByPolicyNumberAndClaimStatus(policyNumber, "REJECTED");
            System.out.println("[PolicyRuleService] Policy status is REJECTED. Previous rejection count: " + rejectionCount);
            
            if (rejectionCount >= 2) {
                // 3rd or more attempt - send to manual review
                System.out.println("[PolicyRuleService] ⚠ 3rd+ claim attempt after 2 rejections - Sending to MANUAL_REVIEW");
                String claimRef = generateRef();
                
                Claim claim = new Claim();
                claim.setClaimReference(claimRef);
                claim.setPolicyNumber(policyNumber);
                claim.setCauseOfDeath(causeOfDeath);
                claim.setDeceasedFullName(deceasedFullName);
                claim.setAiDecision("MANUAL_REVIEW");
                claim.setAiReason("Policy has been rejected " + rejectionCount + " times previously. This is the " + (rejectionCount + 1) + " attempt. Manual review required.");
                claim.setClaimStatus("MANUAL_REVIEW");
                claimRepository.save(claim);
                
                // Update policy status to UNDER_REVIEW
                policy.setStatus("UNDER_REVIEW");
                policyRepository.save(policy);
                
                return new ClaimResponse("MANUAL_REVIEW", "Policy has been rejected " + rejectionCount + " times previously. This claim requires manual review by an underwriter.", claimRef);
            } else {
                // 1st or 2nd attempt - allow retry but warn
                System.out.println("[PolicyRuleService] Policy was previously rejected (attempt " + (rejectionCount + 1) + "). Allowing retry with full AI analysis.");
                // Change status to ACTIVE temporarily to allow processing
                policy.setStatus("ACTIVE");
                policyRepository.save(policy);
            }
        }

        if (!"ACTIVE".equals(policy.getStatus()) && !"REJECTED".equals(policy.getStatus())) {
            System.out.println("[PolicyRuleService] ⚠ Policy is not active: " + policy.getStatus());
            String claimRef = generateRef();
            
            Claim claim = new Claim();
            claim.setClaimReference(claimRef);
            claim.setPolicyNumber(policyNumber);
            claim.setCauseOfDeath(causeOfDeath);
            claim.setDeceasedFullName(deceasedFullName);
            claim.setAiDecision("REJECTED");
            claim.setAiReason("Policy is not active. Current status: " + policy.getStatus());
            claim.setClaimStatus("REJECTED");
            claimRepository.save(claim);
            
            return new ClaimResponse("REJECTED", "Policy is not active. Current status: " + policy.getStatus(), claimRef);
        }

        // ====== STEP 2: PROCESS FILES AND EXTRACT TEXT ======
        // Defensive: Check files map
        if (files == null) {
            System.out.println("[PolicyRuleService] Files map is null");
            return new ClaimResponse("REJECTED", "No files provided.", null);
        }

        // Defensive: Check claimForm is present
        MultipartFile claimForm = files.get("claimForm");
        if (claimForm == null || claimForm.isEmpty()) {
            System.out.println("[PolicyRuleService] claimForm is missing or empty.");
            return new ClaimResponse("REJECTED", "Claim form is required.", null);
        }

        // Try OCR and catch errors
        System.out.println("\n[PolicyRuleService] ========== STARTING OCR EXTRACTION ==========\n");
        StringBuilder allExtractedText = new StringBuilder();
        
        // Add filled form information for AI comparison
        allExtractedText.append("=== FILLED FORM INFORMATION ===\n");
        allExtractedText.append("Policy Number: ").append(policyNumber).append("\n");
        allExtractedText.append("Policy Holder Name: ").append(policyHolderName != null ? policyHolderName : "N/A").append("\n");
        allExtractedText.append("Cause of Death: ").append(causeOfDeath).append("\n");
        allExtractedText.append("Deceased Full Name: ").append(deceasedFullName).append("\n");
        allExtractedText.append("Deceased Email: ").append(deceasedEmail != null ? deceasedEmail : "N/A").append("\n");
        allExtractedText.append("Deceased Mobile: ").append(deceasedMobile != null ? deceasedMobile : "N/A").append("\n");
        allExtractedText.append("Deceased Address: ").append(deceasedAddress != null ? deceasedAddress : "N/A").append("\n");
        allExtractedText.append("Nominee Full Name: ").append(nomineeFullName != null ? nomineeFullName : "N/A").append("\n");
        allExtractedText.append("Nominee Relationship: ").append(nomineeRelationship != null ? nomineeRelationship : "N/A").append("\n");
        allExtractedText.append("Nominee Mobile: ").append(nomineeMobile != null ? nomineeMobile : "N/A").append("\n\n");
        
        // Add policy database information for cross-verification
        allExtractedText.append("=== POLICY DATABASE INFORMATION ===\n");
        allExtractedText.append("Policy Number (DB): ").append(policy.getPolicyNumber()).append("\n");
        allExtractedText.append("Policy Holder Name (DB): ").append(policy.getPolicyHolderName()).append("\n");
        allExtractedText.append("Policy Status (DB): ").append(policy.getStatus()).append("\n");
        allExtractedText.append("Issue Date: ").append(policy.getIssueDate()).append("\n");
        allExtractedText.append("Maturity Date: ").append(policy.getMaturityDate()).append("\n\n");
        
        String claimFormUrl = null, deathCertUrl = null, doctorUrl = null, policeUrl = null;
        try {
            System.out.println("[PolicyRuleService] Uploading claimForm to Azure Blob Storage...");
            claimFormUrl = blobStorageService.uploadFile(claimForm);
            System.out.println("[PolicyRuleService] ClaimForm uploaded: " + claimFormUrl);
            
            System.out.println("[PolicyRuleService] Extracting text from claimForm using Azure Document AI...");
            String extracted = documentAIService.extractTextFromImage(claimForm);
            allExtractedText.append("=== CLAIM FORM DOCUMENT ===\n");
            allExtractedText.append(extracted);
            System.out.println("[PolicyRuleService] ✓ ClaimForm OCR SUCCESS - Extracted " + (extracted != null ? extracted.length() : 0) + " chars");
            System.out.println("[OCR - ClaimForm] >>> " + (extracted != null ? extracted.substring(0, Math.min(200, extracted.length())) : "EMPTY") + "...");
        } catch (Exception e) {
            System.err.println("[PolicyRuleService] ✗ Failed to process claimForm: " + e.getMessage());
            e.printStackTrace();
            // Continue, but extracted text will be empty
        }

        // Defensive: Optional files
        MultipartFile deathCert = files.get("deathCertificate");
        if (deathCert != null && !deathCert.isEmpty()) {
            try {
                System.out.println("[PolicyRuleService] Uploading deathCertificate...");
                deathCertUrl = blobStorageService.uploadFile(deathCert);
                System.out.println("[PolicyRuleService] DeathCertificate uploaded: " + deathCertUrl);
                
                System.out.println("[PolicyRuleService] Extracting text from deathCertificate...");
                String extracted = documentAIService.extractTextFromImage(deathCert);
                allExtractedText.append("\n\n=== DEATH CERTIFICATE DOCUMENT ===\n");
                allExtractedText.append(extracted);
                System.out.println("[PolicyRuleService] ✓ DeathCertificate OCR SUCCESS - Extracted " + (extracted != null ? extracted.length() : 0) + " chars");
                System.out.println("[OCR - DeathCert] >>> " + (extracted != null ? extracted.substring(0, Math.min(200, extracted.length())) : "EMPTY") + "...");
            } catch (Exception e) {
                System.err.println("[PolicyRuleService] ✗ Failed to process deathCertificate: " + e.getMessage());
                e.printStackTrace();
            }
        }
        MultipartFile doctor = files.get("doctorReport");
        if (doctor != null && !doctor.isEmpty()) {
            try {
                System.out.println("[PolicyRuleService] Uploading doctorReport...");
                doctorUrl = blobStorageService.uploadFile(doctor);
                System.out.println("[PolicyRuleService] DoctorReport uploaded: " + doctorUrl);
                
                System.out.println("[PolicyRuleService] Extracting text from doctorReport...");
                String extracted = documentAIService.extractTextFromImage(doctor);
                allExtractedText.append("\n\n=== DOCTOR/HOSPITAL REPORT DOCUMENT ===\n");
                allExtractedText.append(extracted);
                System.out.println("[PolicyRuleService] ✓ DoctorReport OCR SUCCESS - Extracted " + (extracted != null ? extracted.length() : 0) + " chars");
                System.out.println("[OCR - DoctorReport] >>> " + (extracted != null ? extracted.substring(0, Math.min(200, extracted.length())) : "EMPTY") + "...");
            } catch (Exception e) {
                System.err.println("[PolicyRuleService] ✗ Failed to process doctorReport: " + e.getMessage());
                e.printStackTrace();
            }
        }
        MultipartFile police = files.get("policeReport");
        if (police != null && !police.isEmpty()) {
            try {
                System.out.println("[PolicyRuleService] Uploading policeReport...");
                policeUrl = blobStorageService.uploadFile(police);
                System.out.println("[PolicyRuleService] PoliceReport uploaded: " + policeUrl);
                
                System.out.println("[PolicyRuleService] Extracting text from policeReport...");
                String extracted = documentAIService.extractTextFromImage(police);
                allExtractedText.append("\n\n=== POLICE REPORT DOCUMENT ===\n");
                allExtractedText.append(extracted);
                System.out.println("[PolicyRuleService] ✓ PoliceReport OCR SUCCESS - Extracted " + (extracted != null ? extracted.length() : 0) + " chars");
                System.out.println("[OCR - PoliceReport] >>> " + (extracted != null ? extracted.substring(0, Math.min(200, extracted.length())) : "EMPTY") + "...");
            } catch (Exception e) {
                System.err.println("[PolicyRuleService] ✗ Failed to process policeReport: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n[PolicyRuleService] ========== OCR EXTRACTION COMPLETE ==========\n");
        System.out.println("[PolicyRuleService] Total extracted text length: " + allExtractedText.length() + " chars");
        System.out.println("[PolicyRuleService] Document types processed: Claim Form" 
            + (deathCert != null && !deathCert.isEmpty() ? ", Death Certificate" : "")
            + (doctor != null && !doctor.isEmpty() ? ", Doctor Report" : "")
            + (police != null && !police.isEmpty() ? ", Police Report" : ""));
        System.out.println("\n[COMBINED OCR TEXT FROM ALL DOCUMENTS]\n" + allExtractedText.toString() + "\n[END OCR TEXT]\n");

        // ====== STEP 2.5: CRITICAL VALIDATION - POLICY NUMBER AND NAME MATCH ======
        System.out.println("[PolicyRuleService] ========== VALIDATING POLICY NUMBER & NAME CONSISTENCY ==========");
        String extractedText = allExtractedText.toString().toLowerCase();
        
        // Check if policy number appears in the claim form OCR text
        boolean policyNumberFound = extractedText.contains(policyNumber.toLowerCase());
        System.out.println("[PolicyRuleService] Policy number '" + policyNumber + "' found in documents: " + policyNumberFound);
        
        // Check if policy holder name appears in the claim form OCR text (if provided)
        boolean policyHolderNameFound = true;
        if (policyHolderName != null && !policyHolderName.trim().isEmpty()) {
            policyHolderNameFound = extractedText.contains(policyHolderName.toLowerCase());
            System.out.println("[PolicyRuleService] Policy holder name '" + policyHolderName + "' found in documents: " + policyHolderNameFound);
        }
        
        // If either policy number or name is NOT found in documents → REJECT immediately (fraud)
        if (!policyNumberFound || !policyHolderNameFound) {
            System.out.println("[PolicyRuleService] ⚠ CRITICAL MISMATCH DETECTED - Policy number or name in filled form doesn't match documents!");
            System.out.println("[PolicyRuleService] → Policy Number Match: " + policyNumberFound);
            System.out.println("[PolicyRuleService] → Policy Holder Name Match: " + policyHolderNameFound);
            
            String claimRef = generateRef();
            String rejectReason;
            
            if (!policyNumberFound && !policyHolderNameFound) {
                rejectReason = "Critical fraud detected: Both policy number '" + policyNumber + "' and policy holder name '" + policyHolderName + "' in filled form do not match the uploaded claim documents. This indicates potential document forgery or incorrect policy information.";
            } else if (!policyNumberFound) {
                rejectReason = "Critical fraud detected: Policy number '" + policyNumber + "' in filled form does not match the policy number in uploaded claim documents. This indicates potential document forgery or incorrect policy information.";
            } else {
                rejectReason = "Critical fraud detected: Policy holder name '" + policyHolderName + "' in filled form does not match the name in uploaded claim documents. This indicates potential document forgery or incorrect policy information.";
            }
            
            Claim claim = new Claim();
            claim.setClaimReference(claimRef);
            claim.setPolicyNumber(policyNumber);
            claim.setCauseOfDeath(causeOfDeath);
            claim.setClaimFormUrl(claimFormUrl);
            claim.setDeathCertificateUrl(deathCertUrl);
            claim.setDoctorReportUrl(doctorUrl);
            claim.setPoliceReportUrl(policeUrl);
            claim.setDeceasedFullName(deceasedFullName);
            claim.setAiDecision("REJECTED");
            claim.setAiReason(rejectReason);
            claim.setClaimStatus("REJECTED");
            claimRepository.save(claim);
            
            updatePolicyStatus(policy, "REJECTED");
            
            return new ClaimResponse("REJECTED", rejectReason, claimRef);
        }
        
        System.out.println("[PolicyRuleService] ✓ Policy number and name validation PASSED");

        // ====== STEP 3: AI VALIDATION - CHECK IF FILLED INFO MATCHES FILE INFO ======
        System.out.println("[PolicyRuleService] Calling AI to validate filled information matches document information");
        AiDecision aiDecision;
        try {
            aiDecision = claimAiAgentService.analyzeClaim(allExtractedText.toString(), policyNumber);
        } catch (Exception e) {
            System.out.println("[PolicyRuleService] AI failed: " + e.getMessage());
            aiDecision = new AiDecision("MANUAL_REVIEW", "AI analysis failed, manual review required");
        }

        // ---- Save Claim ----
        String claimRef = generateRef();
        Claim claim = new Claim();
        claim.setClaimReference(claimRef);
        claim.setPolicyNumber(policyNumber);
        claim.setCauseOfDeath(causeOfDeath);
        claim.setClaimFormUrl(claimFormUrl);
        claim.setDeathCertificateUrl(deathCertUrl);
        claim.setDoctorReportUrl(doctorUrl);
        claim.setPoliceReportUrl(policeUrl);
        claim.setDeceasedFullName(deceasedFullName);
        claim.setDeceasedEmail(deceasedEmail);
        claim.setDeceasedMobile(deceasedMobile);
        claim.setDeceasedAddress(deceasedAddress);
        claim.setNomineeFullName(nomineeFullName);
        claim.setNomineeRelationship(nomineeRelationship);
        claim.setNomineeMobile(nomineeMobile);
        claim.setAiDecision(aiDecision.getDecision());
        claim.setAiReason(aiDecision.getReason());
        claim.setClaimStatus(aiDecision.getDecision());
        claimRepository.save(claim);
        System.out.println("[PolicyRuleService] Claim saved with ref: " + claimRef);
        // ---- Update Policy ----
        updatePolicyStatus(policy, aiDecision.getDecision());
        System.out.println("[PolicyRuleService] Policy updated to: " + policy.getStatus());
        System.out.println("[PolicyRuleService] Returning decision: " + aiDecision.getDecision());
        return new ClaimResponse(
                aiDecision.getDecision(),
                aiDecision.getReason(),
                claimRef
        );
    }

    // =====================================================

    private void updatePolicyStatus(Policy policy, String decision) {

        if ("APPROVED".equalsIgnoreCase(decision)) {
            policy.setStatus("CLAIMED");
        } else if ("MANUAL_REVIEW".equalsIgnoreCase(decision)) {
            policy.setStatus("UNDER_REVIEW");
        } else {
            policy.setStatus("REJECTED");
        }

        policyRepository.save(policy);
    }

    private String generateRef() {
        return String.format("CLM-%d%04d",
                Year.now().getValue(),
                new Random().nextInt(10000));
    }
}
