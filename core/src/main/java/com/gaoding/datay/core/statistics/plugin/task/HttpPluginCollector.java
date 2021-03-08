package com.gaoding.datay.core.statistics.plugin.task;

import com.gaoding.datay.common.constant.PluginType;
import com.gaoding.datay.common.element.Record;
import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.statistics.communication.Communication;

/**
 * Created by jingxing on 14-9-9.
 */
public class HttpPluginCollector extends AbstractTaskPluginCollector {
    public HttpPluginCollector(Configuration configuration, Communication Communication,
                               PluginType type) {
        super(configuration, Communication, type);
    }

    @Override
    public void collectDirtyRecord(Record dirtyRecord, Throwable t,
                                   String errorMessage) {
        super.collectDirtyRecord(dirtyRecord, t, errorMessage);
    }

}
