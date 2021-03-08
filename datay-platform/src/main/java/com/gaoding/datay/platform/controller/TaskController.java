package com.gaoding.datay.platform.controller;

import com.gaoding.datay.platform.model.PageDto;
import com.gaoding.datay.platform.model.TaskVo;
import com.gaoding.datay.platform.service.TaskService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("tasks")
    public PageDto<TaskVo> findByJobId(Long jobId,int page) {

        return taskService.findByJobId(jobId,page);

    }

    @GetMapping("tasks/log/{id}")
    public Object getTaskLog(@PathVariable("id")Long id){
        return taskService.getTaskLog(id);
    }


}
