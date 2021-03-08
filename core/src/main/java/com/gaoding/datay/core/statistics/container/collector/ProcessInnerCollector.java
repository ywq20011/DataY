package com.gaoding.datay.core.statistics.container.collector;

import com.gaoding.datay.core.statistics.communication.Communication;
import com.gaoding.datay.core.statistics.communication.LocalTGCommunicationManager;

public class ProcessInnerCollector extends AbstractCollector {

    public ProcessInnerCollector(Long jobId) {
        super.setJobId(jobId);
    }

    @Override
    public Communication collectFromTaskGroup() {
        return LocalTGCommunicationManager.getJobCommunication();
    }

}
