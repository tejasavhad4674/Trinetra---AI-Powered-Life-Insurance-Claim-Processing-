package com.tejas.metlife.claimprocessor.repository;

import com.tejas.metlife.claimprocessor.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyNumber(String policyNumber);
    Optional<Policy> findByPolicyNumberAndPolicyHolderNameIgnoreCase(String policyNumber, String policyHolderName);
}
