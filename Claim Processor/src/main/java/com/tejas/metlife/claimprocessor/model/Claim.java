package com.tejas.metlife.claimprocessor.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name="claims")
@Data
public class Claim {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String claimReference;
 private String policyNumber;
 private String claimStatus;
 private String causeOfDeath;

 private String deceasedFullName;
 private String deceasedEmail;
 private String deceasedMobile;
 private String deceasedAddress;

 private String nomineeFullName;
 private String nomineeRelationship;
 private String nomineeMobile;

 private String deathCertificateUrl;
 private String doctorReportUrl;
 private String policeReportUrl;
 private String claimFormUrl;

    private String aiDecision;
    
    @Column(length = 2000)
    private String aiReason;


 private LocalDateTime createdAt = LocalDateTime.now();
}
