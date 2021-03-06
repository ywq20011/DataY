package com.gaoding.datay.rocketmqwriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.shade.com.google.common.collect.Sets;
import com.gaoding.datay.common.element.Column;
import com.gaoding.datay.common.element.Record;
import com.gaoding.datay.common.exception.DataXException;
import com.gaoding.datay.common.plugin.RecordReceiver;
import com.gaoding.datay.common.spi.ErrorCode;
import com.gaoding.datay.common.spi.Writer;
import com.gaoding.datay.common.util.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMqWriter extends Writer {

	public static class Job extends Writer.Job {
		private static final Logger LOG = LoggerFactory.getLogger(Job.class);
		private Configuration originConfig = null;

		@Override
		public void init() {
			this.originConfig = this.getPluginJobConf();

		}

		@Override
		public void prepare() {
			super.prepare();
		}

		@Override
		public List<Configuration> split(int mandatoryNumber) {
			List<Configuration> splitResultConfigs = new ArrayList<Configuration>();
			for (int j = 0; j < mandatoryNumber; j++) {
				splitResultConfigs.add(originConfig.clone());
			}
			return splitResultConfigs;
		}

		@Override
		public void destroy() {
			super.prepare();
		}

	}

	public static class Task extends Writer.Task {

		private static final Logger LOG = LoggerFactory.getLogger(Task.class);
		private Configuration taskConfig;
		private String topic;
		private String ak;
		private String sk;
		private String nameServer;
		private String tag;
		private String orderly;
		private String shardingKey;

		private String fields;
		Properties properties = new Properties();
		private String[] colsAttr = null;
		private Producer producer = null;

		private OrderProducer orderProducer = null;

		@Override
		public void init() {
			this.taskConfig = super.getPluginJobConf();
			this.topic = this.taskConfig.getNecessaryValue("topic", RocketMqErrorCode.BAD_CONFIG_VALUE);
			this.fields = this.taskConfig.getNecessaryValue("fields", RocketMqErrorCode.BAD_CONFIG_VALUE);
			this.ak = this.taskConfig.getNecessaryValue("ak", RocketMqErrorCode.BAD_CONFIG_VALUE);
			this.sk = this.taskConfig.getNecessaryValue("sk", RocketMqErrorCode.BAD_CONFIG_VALUE);
			this.nameServer = this.taskConfig.getNecessaryValue("nameserver", RocketMqErrorCode.BAD_CONFIG_VALUE);
			this.tag = this.taskConfig.getNecessaryValue("tag", RocketMqErrorCode.BAD_CONFIG_VALUE);
			this.orderly = this.taskConfig.getString("orderly");
			this.shardingKey = this.taskConfig.getString("shardingKey");
			colsAttr = fields.split(",");
			if (StringUtils.equalsIgnoreCase("true", orderly) && StringUtils.isBlank(shardingKey)) {
				throw DataXException.asDataXException(RocketMqErrorCode.BAD_CONFIG_VALUE,
						String.format("??????????????????????????????orderly ???true???[%s]????????????????????????????????????????????? .", shardingKey));
			}
			if (StringUtils.equalsIgnoreCase("true", orderly) && !Sets.newHashSet(colsAttr).contains(shardingKey)) {
				throw DataXException.asDataXException(RocketMqErrorCode.BAD_CONFIG_VALUE,
						"??????????????????????????????fields????????????shardingKey.");
			}

			// AccessKeyId ????????????????????????????????????????????????????????????????????????
			properties.put(PropertyKeyConst.AccessKey, ak);
			// AccessKeySecret ????????????????????????????????????????????????????????????????????????
			properties.put(PropertyKeyConst.SecretKey, sk);
			// ??????????????????????????????????????????
			properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "3000");
			// ?????? TCP ?????????????????????????????????????????????????????? TCP ???????????????????????????????????????
			properties.put(PropertyKeyConst.NAMESRV_ADDR, nameServer);

			if (StringUtils.equalsIgnoreCase("true", orderly)) {
				orderProducer = ONSFactory.createOrderProducer(properties);
				orderProducer.start();
			} else {
				producer = ONSFactory.createProducer(properties);
				producer.start();
			}

		}

		@Override
		public void destroy() {
			if (producer != null && !producer.isClosed()) {
				producer.shutdown();
				;
			}
		}

		@Override
		public void startWrite(RecordReceiver recordReceiver) {

			if (StringUtils.equalsIgnoreCase("true", orderly)) {
				doStartWriterOrderly(recordReceiver);
			} else {
				doStartWrite(recordReceiver);
			}

		}

		private void doStartWriterOrderly(RecordReceiver recordReceiver) {

			Record record = null;
			long totla = 0;
			Message producerRecord = null;
			while ((record = recordReceiver.getFromReader()) != null) {
				int recordLength = record.getColumnNumber();
				JSONObject json = new JSONObject(16);
				String shardingValue = null;
				if (0 != recordLength) {
					Column column;
					for (int i = 0; i < recordLength; i++) {
						column = record.getColumn(i);
						String attre = colsAttr[i];
						if (null != column.getRawData()) {
							json.put(attre, column.asString());
						} else {
							json.put(attre, "");
						}
						
						if (shardingValue == null && StringUtils.equals(attre, shardingKey)) {
							shardingValue = json.getString(attre);
						}
						
					}
				}
				String jsonStr = json.toJSONString();
				producerRecord = new Message(topic, tag, jsonStr.toString().getBytes());

				try {
					orderProducer.send(producerRecord,shardingValue);
					totla = totla + 1;
				} catch (Exception e) {
					// retry
					if (retryOrderly(orderProducer, producerRecord,shardingValue)) {
						// ????????????
						totla = totla + 1;
					} else {
						// ??????????????????????????????,????????????
						LOG.error("{}",e);
						getTaskPluginCollector().collectDirtyRecord(record, "??????????????????:" + jsonStr);
					}
				}

			}
			String msg = String.format("task end, write size :%d", totla);
			getTaskPluginCollector().collectMessage("writesize", String.valueOf(totla));
			LOG.info(msg);
			
			
			
		}

		private boolean retryOrderly(OrderProducer orderProducer, Message producerRecord, String shardingValue) {
			int i = 0;
			while (i < 3) {
				try {
					// ????????????????????? ???????????? ????????????
					Thread.sleep(3000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i++;
				try {
					orderProducer.send(producerRecord,shardingValue);
					return true;
				} catch (Exception e) {
					continue;
				}
			}
			return false;
		}

		private void doStartWrite(RecordReceiver recordReceiver) {
			Record record = null;
			long totla = 0;
			Message producerRecord = null;
			while ((record = recordReceiver.getFromReader()) != null) {
				// System.out.println("??????????????????:"+record.toString());//?????????record ??????????????????????????????????????????
				// {"data":[{"byteSize":5,"rawData":"11111","type":"STRING"},{"byteSize":5,"rawData":"11111","type":"STRING"},{"byteSize":5,"rawData":"11111","type":"STRING"},{"byteSize":4,"rawData":"1111","type":"STRING"},{"byteSize":6,"rawData":"111111","type":"STRING"}],"size":5}
				int recordLength = record.getColumnNumber();
				JSONObject json = new JSONObject(16);
				if (0 != recordLength) {
					Column column;
					for (int i = 0; i < recordLength; i++) {
						column = record.getColumn(i);
						String attre = colsAttr[i];
						if (null != column.getRawData()) {
							json.put(attre, column.asString());
						} else {
							json.put(attre, "");
						}
					}
				}
				String jsonStr = json.toJSONString();
				producerRecord = new Message(topic, tag, jsonStr.toString().getBytes());

				try {
					producer.send(producerRecord);
					totla = totla + 1;
				} catch (Exception e) {
					// retry
					if (retry(producer, producerRecord)) {
						// ????????????
						totla = totla + 1;
					} else {
						// ??????????????????????????????,????????????
						getTaskPluginCollector().collectDirtyRecord(record, "??????????????????:" + jsonStr);
					}
				}

			}
			String msg = String.format("task end, write size :%d", totla);
			getTaskPluginCollector().collectMessage("writesize", String.valueOf(totla));
			LOG.info(msg);

		}

		/**
		 * ??????
		 * 
		 * @param producer
		 * @param producerRecord
		 * @return
		 */
		public boolean retry(Producer producer, Message producerRecord) {
			int i = 0;
			while (i < 3) {
				try {
					// ????????????????????? ????????????
					Thread.sleep(3000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i++;
				try {
					producer.send(producerRecord);
					return true;
				} catch (Exception e) {
					continue;
				}
			}
			return false;
		}
	}

	public enum RocketMqErrorCode implements ErrorCode {

		BAD_CONFIG_VALUE("RocketMqWriter-00", "????????????????????????.");

		private final String code;
		private final String description;

		RocketMqErrorCode(String code, String description) {
			this.code = code;
			this.description = description;
		}

		@Override
		public String getCode() {
			return this.code;
		}

		@Override
		public String getDescription() {
			return this.description;
		}

		@Override
		public String toString() {
			return String.format("Code:[%s], Description:[%s]. ", this.code, this.description);
		}

	}

}

