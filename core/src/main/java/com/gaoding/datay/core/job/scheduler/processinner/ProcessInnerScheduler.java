package com.gaoding.datay.core.job.scheduler.processinner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gaoding.datay.common.exception.DataXException;
import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.job.scheduler.AbstractScheduler;
import com.gaoding.datay.core.statistics.container.communicator.AbstractContainerCommunicator;
import com.gaoding.datay.core.taskgroup.TaskGroupContainer;
import com.gaoding.datay.core.taskgroup.runner.TaskGroupContainerRunner;
import com.gaoding.datay.core.util.FrameworkErrorCode;

public abstract class ProcessInnerScheduler extends AbstractScheduler {

    private ExecutorService taskGroupContainerExecutorService;

    public ProcessInnerScheduler(AbstractContainerCommunicator containerCommunicator) {
        super(containerCommunicator);
    }

    @Override
    public void startAllTaskGroup(List<Configuration> configurations) {
        this.taskGroupContainerExecutorService = Executors
                .newFixedThreadPool(configurations.size());

        for (Configuration taskGroupConfiguration : configurations) {
            TaskGroupContainerRunner taskGroupContainerRunner = newTaskGroupContainerRunner(taskGroupConfiguration);
            this.taskGroupContainerExecutorService.execute(taskGroupContainerRunner);
        }

        this.taskGroupContainerExecutorService.shutdown();
    }

    @Override
    public void dealFailedStat(AbstractContainerCommunicator frameworkCollector, Throwable throwable) {
        this.taskGroupContainerExecutorService.shutdownNow();
        throw DataXException.asDataXException(
                FrameworkErrorCode.PLUGIN_RUNTIME_ERROR, throwable);
    }


    @Override
    public void dealKillingStat(AbstractContainerCommunicator frameworkCollector, int totalTasks) {
        //通过进程退出返回码标示状态
        this.taskGroupContainerExecutorService.shutdownNow();
        throw DataXException.asDataXException(FrameworkErrorCode.KILLED_EXIT_VALUE,
                "job killed status");
    }


    private TaskGroupContainerRunner newTaskGroupContainerRunner(
            Configuration configuration) {
        TaskGroupContainer taskGroupContainer = new TaskGroupContainer(configuration);

        return new TaskGroupContainerRunner(taskGroupContainer);
    }

}
