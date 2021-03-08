package com.gaoding.datay.platform.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.gaoding.datay.platform.model.PageDto;
import com.gaoding.datay.platform.model.Task;
import com.gaoding.datay.platform.model.TaskVo;
import com.gaoding.datay.platform.repository.TaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Value("${datay.log.home}")
    private String dataYLogHome;

    @Transactional
    public Task save(Long id) {
        Task task = new Task();
        task.setJobId(id);
        task.setStartTime(new Date());
        task.setStatus("running");
        return taskRepository.save(task);
    }

    public PageDto<TaskVo> findByJobId(Long jobId,int page) {

      //  List<Task> taskList = taskRepository.findByJobId(jobId);
        PageRequest pageRequest = PageRequest.of(page,10);

        Page<Task> taskList = taskRepository.findByJobId(jobId,pageRequest);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<TaskVo> collect = taskList.stream().map(e -> {
            TaskVo taskVo = new TaskVo();
            taskVo.setByteSpeedPerSecond(e.getByteSpeedPerSecond());
            if (e.getEndTime() != null) {
                taskVo.setEndTime(sdf.format(e.getEndTime()));
            }
            if (e.getStartTime() != null) {
                taskVo.setStartTime(sdf.format(e.getStartTime()));
            }
            taskVo.setId(e.getId());
            taskVo.setJobId(e.getJobId());
            taskVo.setRecordSpeedPerSecond(e.getRecordSpeedPerSecond());
            taskVo.setStatus(e.getStatus());
            taskVo.setTotalCosts(e.getTotalCosts());
            taskVo.setTotalErrorRecords(e.getTotalErrorRecords());
            taskVo.setTotalReadRecords(e.getTotalReadRecords());
            return taskVo;
        }).collect(Collectors.toList());

        PageDto<TaskVo> pageDto = new PageDto();
        pageDto.setTotal(taskList.getTotalElements());
        pageDto.setData(collect);
        return pageDto;
    }

    public void update(Task task, Map<String, Object> statistics) {
        // 修改task信息
        task.setByteSpeedPerSecond((Long) statistics.get("byteSpeedPerSecond"));
        task.setEndTime(new Date((Long) statistics.get("endTimeStamp")));
        task.setStartTime(new Date((Long) statistics.get("startTimeStamp")));
        task.setStatus("end");
        task.setRecordSpeedPerSecond((Long) statistics.get("recordSpeedPerSecond"));
        task.setTotalCosts((Long) statistics.get("totalCosts"));
        task.setTotalErrorRecords((Long) statistics.get("totalErrorRecords"));
        task.setTotalReadRecords((Long) statistics.get("totalReadRecords"));
        taskRepository.save(task);
    }

    public String getTaskLog(Long id) {
        String logPath = dataYLogHome + "/" + id + "_info.log";
        File file = new File(logPath);
        if (!file.exists()) {
            return "无日志";
        } else {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                int iAvail = is.available();
                byte[] bytes = new byte[iAvail];
                is.read(bytes);
                is.close();
                return new String(bytes);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    @Transactional(rollbackOn = Throwable.class)
    public void deleteByJobId(Long id) {
        List<Task> tasks = taskRepository.findByJobId(id);
        for(Task task : tasks){
            Long taskId = task.getId();
            File file = new File(dataYLogHome + "/" + taskId+"_info.log");
            file.delete();
        }
        taskRepository.deleteByJobId(id);
    }

}
