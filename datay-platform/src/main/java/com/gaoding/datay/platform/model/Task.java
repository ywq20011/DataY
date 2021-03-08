package com.gaoding.datay.platform.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;

@Table
@Entity
@Data
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long jobId;

    private String status;

    private Long totalCosts;

    private Long byteSpeedPerSecond;

    private Long recordSpeedPerSecond;

    private Long totalReadRecords;

    private Long totalErrorRecords;
   
    private Date startTime;

    private Date endTime;




}
