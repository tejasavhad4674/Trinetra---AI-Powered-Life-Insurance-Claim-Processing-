package com.tejas.metlife.claimprocessor.repository;


import com.tejas.metlife.claimprocessor.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByPolicyNumber(String policyNumber);
    long countByPolicyNumberAndClaimStatus(String policyNumber, String claimStatus);
}
