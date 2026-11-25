package com.enigma.mapper;

import com.enigma.entity.SubmissionClue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SubmissionClueMapper {

    List<SubmissionClue> selectBySubmissionId(@Param("submissionId") Long submissionId);

    int insert(SubmissionClue submissionClue);
}
