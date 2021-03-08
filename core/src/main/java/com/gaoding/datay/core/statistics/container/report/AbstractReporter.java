package com.gaoding.datay.core.statistics.container.report;

import com.gaoding.datay.core.statistics.communication.Communication;

public abstract class AbstractReporter {

    public abstract void reportJobCommunication(Long jobId, Communication communication);

    public abstract void reportTGCommunication(Integer taskGroupId, Communication communication);

}
