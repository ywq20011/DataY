package com.gaoding.datay.platform.controller;

import java.util.Optional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoding.datay.platform.model.Job;
import com.gaoding.datay.platform.model.PutJob;
import com.gaoding.datay.platform.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理
 */
@RestController
public class JobController {

    @Autowired
    private JobService jobService;

    @GetMapping("jobs")
    public Object jobs() {
        return jobService.findAll();
    }

    @GetMapping("jobs/{id}")
    public Object job(@PathVariable(value = "id") Long id) {
        Optional<Job> jobOptional = jobService.findById(id);
        if (jobOptional.isPresent()) {
            return jobOptional.get();
        } else {
            throw new RuntimeException();
        }
    }

    @DeleteMapping("jobs/{id}")
    public void deleteJob(@PathVariable(value = "id") Long id) {
        jobService.deleteJobById(id);
    }

    @PostMapping("jobs")
    public void job(@RequestBody Job job) {
        jobService.save(job);
    }



    @GetMapping("jobs/run/{id}")
    public void runJob(@PathVariable(value = "id") Long id,String params) {
        JSONObject json = JSON.parseObject(params);

        if(json == null){
            jobService.runJob(id,null);
        }else{
            jobService.runJob(id,json.getInnerMap());
        }
    }

    @PutMapping("jobs/{id}")
    public void putJob(@PathVariable(value = "id") Long id,@RequestBody PutJob putJob){

        jobService.updateJobById(id,putJob);
        
    }

}
