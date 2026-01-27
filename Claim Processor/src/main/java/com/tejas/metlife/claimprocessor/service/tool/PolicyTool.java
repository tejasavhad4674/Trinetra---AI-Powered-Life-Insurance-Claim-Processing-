package com.tejas.metlife.claimprocessor.service.tool;

import com.tejas.metlife.claimprocessor.model.Policy;
import com.tejas.metlife.claimprocessor.repository.PolicyRepository;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * LangChain4j Tool: Fetch policy details from database.
 */
@Component
public class PolicyTool {

    private final PolicyRepository policyRepository;

    public PolicyTool(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Tool("Fetch policy details by policy number. Returns policy rules and coverage information.")
    public String getPolicyDetails(String policyNumber) {
        Optional<Policy> opt = policyRepository.findByPolicyNumber(policyNumber);
        
        if (opt.isEmpty()) {
            return "Policy not found for number: " + policyNumber;
        }
        
        Policy policy = opt.get();
        
        return String.format(
            """
            Policy Number: %s
            Policy Holder: %s
            Date of Birth: %s
            Issue Date: %s
            Maturity Date: %s
            Status: %s
            Suicide Coverage After: %d years
            Covers Accident: %s
            Covers Natural Death: %s
            Covers Disease: %s
            """,
            policy.getPolicyNumber(),
            policy.getPolicyHolderName(),
            policy.getDateOfBirth(),
            policy.getIssueDate(),
            policy.getMaturityDate(),
            policy.getStatus(),
            policy.getSuicideCoverageAfterYears(),
            policy.isCoversAccident(),
            policy.isCoversNaturalDeath(),
            policy.isCoversDisease()
        );
    }
}
