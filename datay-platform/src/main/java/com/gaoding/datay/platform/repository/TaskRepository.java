package com.gaoding.datay.platform.repository;

import java.util.List;

import com.gaoding.datay.platform.model.Task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface TaskRepository  extends JpaRepository<Task,Long>{

	List<Task> findByJobId(Long jobId);

    Page<Task> findByJobId( Long jobId, Pageable pageRequest);

    void deleteByJobId(Long id);
    
}
