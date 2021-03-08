package com.gaoding.datay.core.statistics.container.communicator;




import java.util.List;
import java.util.Map;

import com.gaoding.datay.common.statistics.VMInfo;
import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.job.meta.State;
import com.gaoding.datay.core.statistics.communication.Communication;
import com.gaoding.datay.core.statistics.container.collector.AbstractCollector;
import com.gaoding.datay.core.statistics.container.report.AbstractReporter;
import com.gaoding.datay.core.util.container.CoreConstant;

public abstract class AbstractContainerCommunicator {
    private Configuration configuration;
    private AbstractCollector collector;
    private AbstractReporter reporter;

    private Long jobId;

    private VMInfo vmInfo = VMInfo.getVmInfo();
    private long lastReportTime = System.currentTimeMillis();


    public Configuration getConfiguration() {
        return this.configuration;
    }

    public AbstractCollector getCollector() {
        return collector;
    }

    public AbstractReporter getReporter() {
        return reporter;
    }

    public void setCollector(AbstractCollector collector) {
        this.collector = collector;
    }

    public void setReporter(AbstractReporter reporter) {
        this.reporter = reporter;
    }

    public Long getJobId() {
        return jobId;
    }

    public AbstractContainerCommunicator(Configuration configuration) {
        this.configuration = configuration;
        this.jobId = configuration.getLong(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID);
    }


    public abstract void registerCommunication(List<Configuration> configurationList);

    public abstract Communication collect();

    public abstract void report(Communication communication);

    public abstract State collectState();

    public abstract Communication getCommunication(Integer id);

    /**
     * 当 实现是 TGContainerCommunicator 时，返回的 Map: key=taskId, value=Communication
     * 当 实现是 JobContainerCommunicator 时，返回的 Map: key=taskGroupId, value=Communication
     */
    public abstract Map<Integer, Communication> getCommunicationMap();

    public void resetCommunication(Integer id){
        Map<Integer, Communication> map = getCommunicationMap();
        map.put(id, new Communication());
    }

    public void reportVmInfo(){
        long now = System.currentTimeMillis();
        //每5分钟打印一次
        if(now - lastReportTime >= 300000) {
            //当前仅打印
            if (vmInfo != null) {
                vmInfo.getDelta(true);
            }
            lastReportTime = now;
        }
    }
}