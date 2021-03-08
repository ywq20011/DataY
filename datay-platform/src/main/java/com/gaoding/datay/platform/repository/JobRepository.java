package com.gaoding.datay.platform.repository;

import com.gaoding.datay.platform.model.Job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job,Long> {
    



}
