package com.gaoding.datay.core.statistics.container.communicator.taskgroup;

import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.statistics.communication.Communication;
import com.gaoding.datay.core.statistics.container.report.ProcessInnerReporter;

public class StandaloneTGContainerCommunicator extends AbstractTGContainerCommunicator {

    public StandaloneTGContainerCommunicator(Configuration configuration) {
        super(configuration);
        super.setReporter(new ProcessInnerReporter());
    }

    @Override
    public void report(Communication communication) {
        super.getReporter().reportTGCommunication(super.taskGroupId, communication);
    }

}
