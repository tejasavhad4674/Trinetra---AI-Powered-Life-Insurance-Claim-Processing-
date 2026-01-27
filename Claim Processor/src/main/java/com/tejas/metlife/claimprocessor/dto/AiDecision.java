package com.tejas.metlife.claimprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO representing AI agent decision result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDecision {
    private String decision;
    private String reason;
}
