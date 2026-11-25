package com.enigma.entity;

import lombok.Data;

@Data
public class SubmissionClue {

    private Long id;
    private Long submissionId;
    private Long clueId;
    private String relationType;
}
