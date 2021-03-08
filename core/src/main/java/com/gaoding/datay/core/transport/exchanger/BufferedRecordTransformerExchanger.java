package com.gaoding.datay.core.transport.exchanger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.gaoding.datay.common.element.Record;
import com.gaoding.datay.common.exception.CommonErrorCode;
import com.gaoding.datay.common.exception.DataXException;
import com.gaoding.datay.common.plugin.RecordReceiver;
import com.gaoding.datay.common.plugin.RecordSender;
import com.gaoding.datay.common.plugin.TaskPluginCollector;
import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.statistics.communication.Communication;
import com.gaoding.datay.core.transport.channel.Channel;
import com.gaoding.datay.core.transport.record.TerminateRecord;
import com.gaoding.datay.core.transport.transformer.TransformerExecution;
import com.gaoding.datay.core.util.FrameworkErrorCode;
import com.gaoding.datay.core.util.container.CoreConstant;

import org.apache.commons.lang.Validate;

public class BufferedRecordTransformerExchanger extends TransformerExchanger implements RecordSender, RecordReceiver {

    private final Channel channel;

    private final Configuration configuration;

    private final List<Record> buffer;

    private int bufferSize;

    protected final int byteCapacity;

    private final AtomicInteger memoryBytes = new AtomicInteger(0);

    private int bufferIndex = 0;

    private static Class<? extends Record> RECORD_CLASS;

    private volatile boolean shutdown = false;


    @SuppressWarnings("unchecked")
    public BufferedRecordTransformerExchanger(final int taskGroupId, final int taskId,
                                              final Channel channel2, final Communication communication,
                                              final TaskPluginCollector pluginCollector,
                                              final List<TransformerExecution> tInfoExecs) {
        super(taskGroupId, taskId, communication, tInfoExecs, pluginCollector);
        assert null != channel2;
        assert null != channel2.getConfiguration();

        this.channel = channel2;
        this.configuration = channel2.getConfiguration();

        this.bufferSize = configuration
                .getInt(CoreConstant.DATAX_CORE_TRANSPORT_EXCHANGER_BUFFERSIZE);
        this.buffer = new ArrayList<Record>(bufferSize);

        //channel的queue默认大小为8M，原来为64M
        this.byteCapacity = configuration.getInt(
                CoreConstant.DATAX_CORE_TRANSPORT_CHANNEL_CAPACITY_BYTE, 8 * 1024 * 1024);

        try {
            BufferedRecordTransformerExchanger.RECORD_CLASS = ((Class<? extends Record>) Class
                    .forName(configuration.getString(
                            CoreConstant.DATAX_CORE_TRANSPORT_RECORD_CLASS,
                            "com.gaoding.datay.core.transport.record.DefaultRecord")));
        } catch (Exception e) {
            throw DataXException.asDataXException(
                    FrameworkErrorCode.CONFIG_ERROR, e);
        }
    }

    @Override
    public Record createRecord() {
        try {
            return BufferedRecordTransformerExchanger.RECORD_CLASS.newInstance();
        } catch (Exception e) {
            throw DataXException.asDataXException(
                    FrameworkErrorCode.CONFIG_ERROR, e);
        }
    }

    @Override
    public void sendToWriter(Record record) {
        if (shutdown) {
            throw DataXException.asDataXException(CommonErrorCode.SHUT_DOWN_TASK, "");
        }

        Validate.notNull(record, "record不能为空.");

        record = doTransformer(record);

        if(record == null){
            return;
        }

        if (record.getMemorySize() > this.byteCapacity) {
            this.pluginCollector.collectDirtyRecord(record, new Exception(String.format("单条记录超过大小限制，当前限制为:%s", this.byteCapacity)));
            return;
        }

        //当队列满了，需要flush？问题：flush后的数据去哪了？
        boolean isFull = (this.bufferIndex >= this.bufferSize || this.memoryBytes.get() + record.getMemorySize() > this.byteCapacity);
        if (isFull) {
            //将缓冲区的数据flush到队列。
            flush();
        }

        this.buffer.add(record);
        this.bufferIndex++;
        memoryBytes.addAndGet(record.getMemorySize());
    }

    @Override
    public void flush() {
        if (shutdown) {
            throw DataXException.asDataXException(CommonErrorCode.SHUT_DOWN_TASK, "");
        }
        // 这一步，发生了什么？
        this.channel.pushAll(this.buffer);
        //和channel的统计保持同步
        doStat();
        this.buffer.clear();
        this.bufferIndex = 0;
        this.memoryBytes.set(0);
    }

    @Override
    public void terminate() {
        if (shutdown) {
            throw DataXException.asDataXException(CommonErrorCode.SHUT_DOWN_TASK, "");
        }
        flush();
        this.channel.pushTerminate(TerminateRecord.get());
    }

    @Override
    public Record getFromReader() {
        if (shutdown) {
            throw DataXException.asDataXException(CommonErrorCode.SHUT_DOWN_TASK, "");
        }
        boolean isEmpty = (this.bufferIndex >= this.buffer.size());
        if (isEmpty) {
            // buffer 为空的时候，调用 receive ？发生了什么？
            // receive 后，buffer 又有数据了。那这个 channel 的存在意义是啥？？？ 答案是流速控制
            receive();
        }

        Record record = this.buffer.get(this.bufferIndex++);
        if (record instanceof TerminateRecord) {
            record = null;
        }
        return record;
    }

    @Override
    public void shutdown() {
        shutdown = true;
        try {
            buffer.clear();
            channel.clear();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void receive() {
        // pullAll发生了什么？ 又把数据丢到缓冲区了？？那这个队列的意义是啥？？？答案是流速控制和统计。
        this.channel.pullAll(this.buffer);
        this.bufferIndex = 0;
        this.bufferSize = this.buffer.size();
    }

}
