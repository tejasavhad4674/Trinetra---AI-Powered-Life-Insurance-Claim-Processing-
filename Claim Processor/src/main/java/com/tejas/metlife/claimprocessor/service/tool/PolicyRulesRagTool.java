package com.tejas.metlife.claimprocessor.service.tool;

import com.tejas.metlife.claimprocessor.service.PolicyRagService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * LangChain4j Tool: Retrieve relevant policy rules using RAG.
 */
@Component
public class PolicyRulesRagTool {

    private final PolicyRagService policyRagService;

    public PolicyRulesRagTool(PolicyRagService policyRagService) {
        this.policyRagService = policyRagService;
    }

    @Tool("Retrieve relevant policy rules and constraints based on the query. Use this to understand what the policy covers, exclusions, required documents, and processing rules.")
    public String retrievePolicyRules(String query) {
        System.out.println("[PolicyRulesRagTool] Query: " + query);
        
        if (!policyRagService.isRagEnabled()) {
            System.out.println("[PolicyRulesRagTool] ⚠ RAG is disabled - returning general guidelines");
            return """
                General MetLife Insurance Policy Guidelines (RAG unavailable):
                - Active policies required for claims
                - Claims must be filed within 30 days of death
                - Suicide generally excluded in first year
                - Accidental death requires police report
                - Natural death requires death certificate
                - All documents must be genuine and verifiable
                - Non-existent hospitals/police stations indicate fraud
                """;
        }
        
        // Retrieve top 5 most relevant policy rule segments
        String relevantRules = policyRagService.retrieveRelevantPolicyRules(query, 5);
        
        if (relevantRules == null || relevantRules.isEmpty()) {
            return "No specific policy rules found for this query. Apply general insurance claim guidelines.";
        }
        
        return "Relevant Policy Rules:\n\n" + relevantRules;
    }
}
