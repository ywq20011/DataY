package com.gaoding.datay.platform.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import com.gaoding.datay.platform.model.Job;
import com.gaoding.datay.platform.model.PutJob;
import com.gaoding.datay.platform.model.Task;
import com.gaoding.datay.platform.repository.JobRepository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(\\$)\\{?(\\w+)\\}?");

    @Autowired
    private TaskService taskService;

    public Object findAll() {

        return jobRepository.findAll();
    }

    public Optional<Job> findById(Long id) {
        return jobRepository.findById(id);
    }

    public void save(Job job) {
        jobRepository.save(job);
    }

    public void runJob(Long id, Map<String, Object> paramMap) {
        Optional<Job> jobOptional = jobRepository.findById(id);
        if (jobOptional.isPresent()) {
            // 保存一条task。
            Task task = taskService.save(id);
            Job job = jobOptional.get();

            // param
            if (paramMap != null && paramMap.size() > 0) {
                String config = replaceVariable(job.getJobConfig(), paramMap);
                job.setJobConfig(config);
            }
            Thread thread = new Thread(new DataXTask(taskService, job, task));
            thread.start();
        }
    }

    public String replaceVariable(final String param, final Map<String, Object> paramMap) {
        Map<String, String> mapping = new HashMap<String, String>();

        Matcher matcher = VARIABLE_PATTERN.matcher(param);
        while (matcher.find()) {
            String variable = matcher.group(2);
            String value = paramMap.get(variable).toString();
            if (StringUtils.isBlank(value)) {// 有就使用环境变量替换，没有则$param占位符形式
                value = matcher.group();
            }
            mapping.put(matcher.group(), value);
        }

        String retString = param;
        for (final String key : mapping.keySet()) {
            retString = retString.replace(key, mapping.get(key));
        }

        return retString;
    }

    @Transactional(rollbackOn = Throwable.class)
    public void deleteJobById(Long id) {
        //删除job
        jobRepository.deleteById(id);
        //删除task

        taskService.deleteByJobId(id);

    }

    @Transactional(rollbackOn = Throwable.class)
    public void updateJobById(Long id, PutJob putJob) {
        Optional<Job> optional = jobRepository.findById(id);
        if (optional.isPresent()) {
            Job job = optional.get();
            job.setJobConfig(putJob.getJobConfig());
            job.setJobName(putJob.getJobName());
            jobRepository.save(job);
        }
    }

}
