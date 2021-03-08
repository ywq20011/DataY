package com.gaoding.datay.platform.service;

import java.util.Map;

import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.Engine;
import com.gaoding.datay.core.job.JobContainer;
import com.gaoding.datay.core.util.ConfigParser;
import com.gaoding.datay.core.util.container.CoreConstant;
import com.gaoding.datay.platform.model.Job;
import com.gaoding.datay.platform.model.Task;

import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class DataXTask implements Runnable {

    private TaskService taskService;

    private Job job;
    private Task task;

    public DataXTask(TaskService taskService,Job job,Task task){
        this.taskService = taskService;
        this.job = job;
        this.task = task;
    }


    @Override
    public void run() {
        Engine engine = new Engine();
        Configuration configuration = ConfigParser.parse(job.getJobConfig());
        configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, task.getId());
        try {
            engine.start(configuration,task.getId()+"");
        } catch (Exception e) {
            log.warn("{}",e.getMessage());
        }finally{
            JobContainer container = (JobContainer) engine.getContainer();
            Map<String, Object> statistics = container.getStatistics();
            taskService.update(task, statistics);
        }

        
    }
    


}
