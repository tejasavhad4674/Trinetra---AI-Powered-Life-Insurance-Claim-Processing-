package com.tejas.metlife.claimprocessor.controller;

import com.tejas.metlife.claimprocessor.dto.ClaimResponse;
import com.tejas.metlife.claimprocessor.service.PolicyRuleService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/claim")
public class ClaimController {

    private final PolicyRuleService policyRuleService;

    public ClaimController(PolicyRuleService policyRuleService) {
        this.policyRuleService = policyRuleService;
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimResponse> submitClaim(

        @RequestParam(required = false) String policyNumber,
        @RequestParam(required = false) String policyHolderName,
        @RequestParam(required = false) String causeOfDeath,

        @RequestParam(required = false) String deceasedFullName,
        @RequestParam(required = false) String deceasedEmail,
        @RequestParam(required = false) String deceasedMobile,
        @RequestParam(required = false) String deceasedAddress,

        @RequestParam(required = false) String nomineeFullName,
        @RequestParam(required = false) String nomineeRelationship,
        @RequestParam(required = false) String nomineeMobile,

        @RequestParam(required = false) MultipartFile claimForm,
        @RequestParam(required = false) MultipartFile deathCertificate,
        @RequestParam(required = false) MultipartFile doctorReport,
        @RequestParam(required = false) MultipartFile policeReport
    ) {

        Map<String, MultipartFile> files = new HashMap<>();
        files.put("claimForm", claimForm);
        files.put("deathCertificate", deathCertificate);
        files.put("doctorReport", doctorReport);
        files.put("policeReport", policeReport);

        // ---- Logging uploaded files ----
        System.out.println("[ClaimController] Received claim submission:");
        System.out.println("  Policy Number: " + policyNumber);
        System.out.println("  Policy Holder: " + policyHolderName);
        System.out.println("  Deceased: " + deceasedFullName);
        System.out.println("[ClaimController] Uploaded files:");
        files.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                System.out.println(" - " + k + ": " + v.getOriginalFilename() + " (" + v.getSize() + " bytes)");
            } else {
                System.out.println(" - " + k + ": <not provided>");
            }
        });

        // ---- Call Rule + AI Service ----
        ClaimResponse resp = policyRuleService.evaluate(
            policyNumber,
            policyHolderName,
            causeOfDeath,
            files,
            deceasedFullName,
            deceasedEmail,
            deceasedMobile,
            deceasedAddress,
            nomineeFullName,
            nomineeRelationship,
            nomineeMobile
        );
        
        System.out.println("[ClaimController] Response: " + resp.getStatus() + " - " + resp.getMessage());
        
        // Return 200 OK for all responses (including REJECTED) so frontend can read the response
        return ResponseEntity.ok(resp);
    }
}
