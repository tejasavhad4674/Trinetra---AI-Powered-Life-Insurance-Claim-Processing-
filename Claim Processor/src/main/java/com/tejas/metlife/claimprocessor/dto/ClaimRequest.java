package com.tejas.metlife.claimprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClaimRequest {

    private String deceasedFullName;
    private String deceasedEmail;
    private String deceasedMobile;
    private String deceasedAddress;

    private String nomineeFullName;
    private String nomineeRelationship;
    private String nomineeMobile;
}
