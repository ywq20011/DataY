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
						String.format("您提供配置文件有误，orderly 为true，[%s]是必填参数，不允许为空或者留白 .", shardingKey));
			}
			if (StringUtils.equalsIgnoreCase("true", orderly) && !Sets.newHashSet(colsAttr).contains(shardingKey)) {
				throw DataXException.asDataXException(RocketMqErrorCode.BAD_CONFIG_VALUE,
						"您提供配置文件有误，fields需要包含shardingKey.");
			}

			// AccessKeyId 阿里云身份验证，在阿里云用户信息管理控制台获取。
			properties.put(PropertyKeyConst.AccessKey, ak);
			// AccessKeySecret 阿里云身份验证，在阿里云用户信息管理控制台获取。
			properties.put(PropertyKeyConst.SecretKey, sk);
			// 设置发送超时时间，单位毫秒。
			properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "3000");
			// 设置 TCP 接入域名，进入控制台的实例详情页面的 TCP 协议客户端接入点区域查看。
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
						// 发送成功
						totla = totla + 1;
					} else {
						// 实在发不动，发送失败,记录日志
						LOG.error("{}",e);
						getTaskPluginCollector().collectDirtyRecord(record, "消息发送失败:" + jsonStr);
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
					// 可能是网络波动 停止三秒 重试三次
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
				// System.out.println("获得的数据为:"+record.toString());//这里的record 根据不同的数据库需要进行加工
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
						// 发送成功
						totla = totla + 1;
					} else {
						// 实在发不动，发送失败,记录日志
						getTaskPluginCollector().collectDirtyRecord(record, "消息发送失败:" + jsonStr);
					}
				}

			}
			String msg = String.format("task end, write size :%d", totla);
			getTaskPluginCollector().collectMessage("writesize", String.valueOf(totla));
			LOG.info(msg);

		}

		/**
		 * 重试
		 * 
		 * @param producer
		 * @param producerRecord
		 * @return
		 */
		public boolean retry(Producer producer, Message producerRecord) {
			int i = 0;
			while (i < 3) {
				try {
					// 可能是网络波动 停止三秒
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

		BAD_CONFIG_VALUE("RocketMqWriter-00", "您配置的值不合法.");

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

