package com.tejas.metlife.claimprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {
    private String status;
    private String message;
    private String claimReference;
}
