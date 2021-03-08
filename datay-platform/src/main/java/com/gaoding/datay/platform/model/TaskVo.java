package com.gaoding.datay.platform.model;

import lombok.Data;

@Data
public class TaskVo{

    private Long id;
    
    private Long jobId;

    private String status;

    private Long totalCosts;

    private Long byteSpeedPerSecond;

    private Long recordSpeedPerSecond;

    private Long totalReadRecords;

    private Long totalErrorRecords;
   
    private String startTime;

    private String endTime;

}