package com.gaoding.datay.core.statistics.container.communicator.taskgroup;


import java.util.List;
import java.util.Map;

import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.job.meta.State;
import com.gaoding.datay.core.statistics.communication.Communication;
import com.gaoding.datay.core.statistics.container.collector.ProcessInnerCollector;
import com.gaoding.datay.core.statistics.container.communicator.AbstractContainerCommunicator;
import com.gaoding.datay.core.util.container.CoreConstant;

import org.apache.commons.lang.Validate;

/**
 * 该类是用于处理 taskGroupContainer 的 communication 的收集汇报的父类
 * 主要是 taskCommunicationMap 记录了 taskExecutor 的 communication 属性
 */
public abstract class AbstractTGContainerCommunicator extends AbstractContainerCommunicator {

    protected long jobId;

    /**
     * 由于taskGroupContainer是进程内部调度
     * 其registerCommunication()，getCommunication()，
     * getCommunications()，collect()等方法是一致的
     * 所有TG的Collector都是ProcessInnerCollector
     */
    protected int taskGroupId;

    public AbstractTGContainerCommunicator(Configuration configuration) {
        super(configuration);
        this.jobId = configuration.getInt(
                CoreConstant.DATAX_CORE_CONTAINER_JOB_ID);
        super.setCollector(new ProcessInnerCollector(this.jobId));
        this.taskGroupId = configuration.getInt(
                CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_ID);
    }

    @Override
    public void registerCommunication(List<Configuration> configurationList) {
        super.getCollector().registerTaskCommunication(configurationList);
    }

    @Override
    public final Communication collect() {
        return this.getCollector().collectFromTask();
    }

    @Override
    public final State collectState() {
        Communication communication = new Communication();
        communication.setState(State.SUCCEEDED);

        for (Communication taskCommunication :
                super.getCollector().getTaskCommunicationMap().values()) {
            communication.mergeStateFrom(taskCommunication);
        }

        return communication.getState();
    }

    @Override
    public final Communication getCommunication(Integer taskId) {
        Validate.isTrue(taskId >= 0, "注册的taskId不能小于0");

        return super.getCollector().getTaskCommunication(taskId);
    }

    @Override
    public final Map<Integer, Communication> getCommunicationMap() {
        return super.getCollector().getTaskCommunicationMap();
    }

}
