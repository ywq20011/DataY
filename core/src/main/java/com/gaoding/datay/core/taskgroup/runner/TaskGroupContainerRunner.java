package com.gaoding.datay.core.taskgroup.runner;

import com.gaoding.datay.common.exception.DataXException;
import com.gaoding.datay.core.job.meta.State;
import com.gaoding.datay.core.taskgroup.TaskGroupContainer;
import com.gaoding.datay.core.util.FrameworkErrorCode;

public class TaskGroupContainerRunner implements Runnable {

	private TaskGroupContainer taskGroupContainer;

	private State state;

	public TaskGroupContainerRunner(TaskGroupContainer taskGroup) {
		this.taskGroupContainer = taskGroup;
		this.state = State.SUCCEEDED;
	}

	@Override
	public void run() {
		try {
            Thread.currentThread().setName(
                    String.format("taskGroup-%d", this.taskGroupContainer.getTaskGroupId()));
            this.taskGroupContainer.start();
			this.state = State.SUCCEEDED;
		} catch (Throwable e) {
			this.state = State.FAILED;
			throw DataXException.asDataXException(
					FrameworkErrorCode.RUNTIME_ERROR, e);
		}
	}

	public TaskGroupContainer getTaskGroupContainer() {
		return taskGroupContainer;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
}
