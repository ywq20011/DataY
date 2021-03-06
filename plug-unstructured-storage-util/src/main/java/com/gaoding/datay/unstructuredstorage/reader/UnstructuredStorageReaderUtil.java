package com.gaoding.datay.unstructuredstorage.reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.csvreader.CsvReader;
import com.gaoding.datay.common.element.BoolColumn;
import com.gaoding.datay.common.element.Column;
import com.gaoding.datay.common.element.DateColumn;
import com.gaoding.datay.common.element.DoubleColumn;
import com.gaoding.datay.common.element.LongColumn;
import com.gaoding.datay.common.element.Record;
import com.gaoding.datay.common.element.StringColumn;
import com.gaoding.datay.common.exception.DataXException;
import com.gaoding.datay.common.plugin.RecordSender;
import com.gaoding.datay.common.plugin.TaskPluginCollector;
import com.gaoding.datay.common.util.Configuration;

import org.anarres.lzo.LzoDecompressor1x_safe;
import org.anarres.lzo.LzoInputStream;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.airlift.compress.snappy.SnappyCodec;
import io.airlift.compress.snappy.SnappyFramedInputStream;

public class UnstructuredStorageReaderUtil {
	private static final Logger LOG = LoggerFactory
			.getLogger(UnstructuredStorageReaderUtil.class);
	public static HashMap<String, Object> csvReaderConfigMap;

	private UnstructuredStorageReaderUtil() {

	}

	/**
	 * @param inputLine
	 *            ????????????????????????
	 * @param delimiter
	 *            ??????????????????
	 * @return ???????????????????????????????????????????????????????????????null ?????????????????????????????????????????????
	 * */
	public static String[] splitOneLine(String inputLine, char delimiter) {
		String[] splitedResult = null;
		if (null != inputLine) {
			try {
				CsvReader csvReader = new CsvReader(new StringReader(inputLine));
				csvReader.setDelimiter(delimiter);

				setCsvReaderConfig(csvReader);

				if (csvReader.readRecord()) {
					splitedResult = csvReader.getValues();
				}
			} catch (IOException e) {
				// nothing to do
			}
		}
		return splitedResult;
	}

	public static String[] splitBufferedReader(CsvReader csvReader)
			throws IOException {
		String[] splitedResult = null;
		if (csvReader.readRecord()) {
			splitedResult = csvReader.getValues();
		}
		return splitedResult;
	}

	/**
	 * ???????????????
	 *
	 * @return ????????????????????????????????????
	 * */
	public static String[] splitOneLine(String inputLine, String delimiter) {
		String[] splitedResult = StringUtils.split(inputLine, delimiter);
		return splitedResult;
	}

	public static void readFromStream(InputStream inputStream, String context,
									  Configuration readerSliceConfig, RecordSender recordSender,
									  TaskPluginCollector taskPluginCollector) {
		String compress = readerSliceConfig.getString(Key.COMPRESS, null);
		if (StringUtils.isBlank(compress)) {
			compress = null;
		}
		String encoding = readerSliceConfig.getString(Key.ENCODING,
				Constant.DEFAULT_ENCODING);
		// handle blank encoding
		if (StringUtils.isBlank(encoding)) {
			encoding = Constant.DEFAULT_ENCODING;
			LOG.warn(String.format("????????????encoding???[%s], ???????????????[%s]", encoding,
					Constant.DEFAULT_ENCODING));
		}

		List<Configuration> column = readerSliceConfig
				.getListConfiguration(Key.COLUMN);
		// handle ["*"] -> [], null
		if (null != column && 1 == column.size()
				&& "\"*\"".equals(column.get(0).toString())) {
			readerSliceConfig.set(Key.COLUMN, null);
			column = null;
		}

		BufferedReader reader = null;
		int bufferSize = readerSliceConfig.getInt(Key.BUFFER_SIZE,
				Constant.DEFAULT_BUFFER_SIZE);

		// compress logic
		try {
			if (null == compress) {
				reader = new BufferedReader(new InputStreamReader(inputStream,
						encoding), bufferSize);
			} else {
				// TODO compress
				if ("lzo_deflate".equalsIgnoreCase(compress)) {
					LzoInputStream lzoInputStream = new LzoInputStream(
							inputStream, new LzoDecompressor1x_safe());
					reader = new BufferedReader(new InputStreamReader(
							lzoInputStream, encoding));
				} else if ("lzo".equalsIgnoreCase(compress)) {
					LzoInputStream lzopInputStream = new ExpandLzopInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							lzopInputStream, encoding));
				} else if ("gzip".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new GzipCompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding), bufferSize);
				} else if ("bzip2".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new BZip2CompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding), bufferSize);
				} else if ("hadoop-snappy".equalsIgnoreCase(compress)) {
					CompressionCodec snappyCodec = new SnappyCodec();
					InputStream snappyInputStream = snappyCodec.createInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							snappyInputStream, encoding));
				} else if ("framing-snappy".equalsIgnoreCase(compress)) {
					InputStream snappyInputStream = new SnappyFramedInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							snappyInputStream, encoding));
				}/* else if ("lzma".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new LZMACompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding));
				} *//*else if ("pack200".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new Pack200CompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding));
				} *//*else if ("xz".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new XZCompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding));
				} else if ("ar".equalsIgnoreCase(compress)) {
					ArArchiveInputStream arArchiveInputStream = new ArArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							arArchiveInputStream, encoding));
				} else if ("arj".equalsIgnoreCase(compress)) {
					ArjArchiveInputStream arjArchiveInputStream = new ArjArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							arjArchiveInputStream, encoding));
				} else if ("cpio".equalsIgnoreCase(compress)) {
					CpioArchiveInputStream cpioArchiveInputStream = new CpioArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							cpioArchiveInputStream, encoding));
				} else if ("dump".equalsIgnoreCase(compress)) {
					DumpArchiveInputStream dumpArchiveInputStream = new DumpArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							dumpArchiveInputStream, encoding));
				} else if ("jar".equalsIgnoreCase(compress)) {
					JarArchiveInputStream jarArchiveInputStream = new JarArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							jarArchiveInputStream, encoding));
				} else if ("tar".equalsIgnoreCase(compress)) {
					TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							tarArchiveInputStream, encoding));
				}*/
				else if ("zip".equalsIgnoreCase(compress)) {
					ZipCycleInputStream zipCycleInputStream = new ZipCycleInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							zipCycleInputStream, encoding), bufferSize);
				} else {
					throw DataXException
							.asDataXException(
									UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
									String.format("????????? gzip, bzip2, zip, lzo, lzo_deflate, hadoop-snappy, framing-snappy" +
											"?????????????????? , ???????????????????????????????????????: [%s]", compress));
				}
			}
			UnstructuredStorageReaderUtil.doReadFromStream(reader, context,
					readerSliceConfig, recordSender, taskPluginCollector);
		} catch (UnsupportedEncodingException uee) {
			throw DataXException
					.asDataXException(
							UnstructuredStorageReaderErrorCode.OPEN_FILE_WITH_CHARSET_ERROR,
							String.format("???????????????????????? : [%s]", encoding), uee);
		} catch (NullPointerException e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.RUNTIME_EXCEPTION,
					"???????????????, ???????????????", e);
		}/* catch (ArchiveException e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.READ_FILE_IO_ERROR,
					String.format("??????????????????????????? : [%s]", context), e);
		} */catch (IOException e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.READ_FILE_IO_ERROR,
					String.format("??????????????? : [%s]", context), e);
		} finally {
			IOUtils.closeQuietly(reader);
		}

	}

	public static void doReadFromStream(BufferedReader reader, String context,
										Configuration readerSliceConfig, RecordSender recordSender,
										TaskPluginCollector taskPluginCollector) {
		String encoding = readerSliceConfig.getString(Key.ENCODING,
				Constant.DEFAULT_ENCODING);
		Character fieldDelimiter = null;
		String delimiterInStr = readerSliceConfig
				.getString(Key.FIELD_DELIMITER);
		if (null != delimiterInStr && 1 != delimiterInStr.length()) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("???????????????????????????, ????????????????????? : [%s]", delimiterInStr));
		}
		if (null == delimiterInStr) {
			LOG.warn(String.format("???????????????????????????, ???????????????[%s]",
					Constant.DEFAULT_FIELD_DELIMITER));
		}

		// warn: default value ',', fieldDelimiter could be \n(lineDelimiter)
		// for no fieldDelimiter
		fieldDelimiter = readerSliceConfig.getChar(Key.FIELD_DELIMITER,
				Constant.DEFAULT_FIELD_DELIMITER);
		Boolean skipHeader = readerSliceConfig.getBool(Key.SKIP_HEADER,
				Constant.DEFAULT_SKIP_HEADER);
		// warn: no default value '\N'
		String nullFormat = readerSliceConfig.getString(Key.NULL_FORMAT);

		// warn: Configuration -> List<ColumnEntry> for performance
		// List<Configuration> column = readerSliceConfig
		// .getListConfiguration(Key.COLUMN);
		List<ColumnEntry> column = UnstructuredStorageReaderUtil
				.getListColumnEntry(readerSliceConfig, Key.COLUMN);
		CsvReader csvReader  = null;

		// every line logic
		try {
			// TODO lineDelimiter
			if (skipHeader) {
				String fetchLine = reader.readLine();
				LOG.info(String.format("Header line %s has been skiped.",
						fetchLine));
			}
			csvReader = new CsvReader(reader);
			csvReader.setDelimiter(fieldDelimiter);

			setCsvReaderConfig(csvReader);

			String[] parseRows;
			while ((parseRows = UnstructuredStorageReaderUtil
					.splitBufferedReader(csvReader)) != null) {
				UnstructuredStorageReaderUtil.transportOneRecord(recordSender,
						column, parseRows, nullFormat, taskPluginCollector);
			}
		} catch (UnsupportedEncodingException uee) {
			throw DataXException
					.asDataXException(
							UnstructuredStorageReaderErrorCode.OPEN_FILE_WITH_CHARSET_ERROR,
							String.format("???????????????????????? : [%s]", encoding), uee);
		} catch (FileNotFoundException fnfe) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.FILE_NOT_EXISTS,
					String.format("?????????????????? : [%s]", context), fnfe);
		} catch (IOException ioe) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.READ_FILE_IO_ERROR,
					String.format("?????????????????? : [%s]", context), ioe);
		} catch (Exception e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.RUNTIME_EXCEPTION,
					String.format("??????????????? : %s", e.getMessage()), e);
		} finally {
			csvReader.close();
			IOUtils.closeQuietly(reader);
		}
	}

	public static Record transportOneRecord(RecordSender recordSender,
											Configuration configuration,
											TaskPluginCollector taskPluginCollector,
											String line){
		List<ColumnEntry> column = UnstructuredStorageReaderUtil
				.getListColumnEntry(configuration, Key.COLUMN);
		// ??????: nullFormat ???????????????
		String nullFormat = configuration.getString(Key.NULL_FORMAT);
		String delimiterInStr = configuration.getString(Key.FIELD_DELIMITER);
		if (null != delimiterInStr && 1 != delimiterInStr.length()) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("???????????????????????????, ????????????????????? : [%s]", delimiterInStr));
		}
		if (null == delimiterInStr) {
			LOG.warn(String.format("???????????????????????????, ???????????????[%s]",
					Constant.DEFAULT_FIELD_DELIMITER));
		}
		// warn: default value ',', fieldDelimiter could be \n(lineDelimiter)
		// for no fieldDelimiter
		Character fieldDelimiter = configuration.getChar(Key.FIELD_DELIMITER,
				Constant.DEFAULT_FIELD_DELIMITER);

		String[] sourceLine = StringUtils.split(line, fieldDelimiter);

		return transportOneRecord(recordSender, column, sourceLine, nullFormat, taskPluginCollector);
	}

	public static Record transportOneRecord(RecordSender recordSender,
											List<ColumnEntry> columnConfigs, String[] sourceLine,
											String nullFormat, TaskPluginCollector taskPluginCollector) {
		Record record = recordSender.createRecord();
		Column columnGenerated = null;

		// ????????????String??????column???record
		if (null == columnConfigs || columnConfigs.size() == 0) {
			for (String columnValue : sourceLine) {
				// not equalsIgnoreCase, it's all ok if nullFormat is null
				if (columnValue.equals(nullFormat)) {
					columnGenerated = new StringColumn(null);
				} else {
					columnGenerated = new StringColumn(columnValue);
				}
				record.addColumn(columnGenerated);
			}
			recordSender.sendToWriter(record);
		} else {
			try {
				for (ColumnEntry columnConfig : columnConfigs) {
					String columnType = columnConfig.getType();
					Integer columnIndex = columnConfig.getIndex();
					String columnConst = columnConfig.getValue();

					String columnValue = null;

					if (null == columnIndex && null == columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.NO_INDEX_VALUE,
										"??????????????????type, ????????????????????? index ??? value");
					}

					if (null != columnIndex && null != columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.MIXED_INDEX_VALUE,
										"??????????????????index, value, ???????????????????????????????????????");
					}

					if (null != columnIndex) {
						if (columnIndex >= sourceLine.length) {
							String message = String
									.format("???????????????????????????,?????????????????? [%s] ???,?????????????????? [%s] ???, ????????????[%s]",
											sourceLine.length, columnIndex + 1,
											StringUtils.join(sourceLine, ","));
							LOG.warn(message);
							throw new IndexOutOfBoundsException(message);
						}

						columnValue = sourceLine[columnIndex];
					} else {
						columnValue = columnConst;
					}
					Type type = Type.valueOf(columnType.toUpperCase());
					// it's all ok if nullFormat is null
					if (columnValue.equals(nullFormat)) {
						columnValue = null;
					}
					switch (type) {
						case STRING:
							columnGenerated = new StringColumn(columnValue);
							break;
						case LONG:
							try {
								columnGenerated = new LongColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"??????????????????, ?????????[%s] ?????????[%s]", columnValue,
										"LONG"));
							}
							break;
						case DOUBLE:
							try {
								columnGenerated = new DoubleColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"??????????????????, ?????????[%s] ?????????[%s]", columnValue,
										"DOUBLE"));
							}
							break;
						case BOOLEAN:
							try {
								columnGenerated = new BoolColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"??????????????????, ?????????[%s] ?????????[%s]", columnValue,
										"BOOLEAN"));
							}

							break;
						case DATE:
							try {
								if (columnValue == null) {
									Date date = null;
									columnGenerated = new DateColumn(date);
								} else {
									String formatString = columnConfig.getFormat();
									//if (null != formatString) {
									if (StringUtils.isNotBlank(formatString)) {
										// ?????????????????????????????????, ???????????????????????????
										DateFormat format = columnConfig
												.getDateFormat();
										columnGenerated = new DateColumn(
												format.parse(columnValue));
									} else {
										// ??????????????????
										columnGenerated = new DateColumn(
												new StringColumn(columnValue)
														.asDate());
									}
								}
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"??????????????????, ?????????[%s] ?????????[%s]", columnValue,
										"DATE"));
							}
							break;
						default:
							String errorMessage = String.format(
									"????????????????????????????????? : [%s]", columnType);
							LOG.error(errorMessage);
							throw DataXException
									.asDataXException(
											UnstructuredStorageReaderErrorCode.NOT_SUPPORT_TYPE,
											errorMessage);
					}

					record.addColumn(columnGenerated);

				}
				recordSender.sendToWriter(record);
			} catch (IllegalArgumentException iae) {
				taskPluginCollector
						.collectDirtyRecord(record, iae.getMessage());
			} catch (IndexOutOfBoundsException ioe) {
				taskPluginCollector
						.collectDirtyRecord(record, ioe.getMessage());
			} catch (Exception e) {
				if (e instanceof DataXException) {
					throw (DataXException) e;
				}
				// ??????????????????????????????????????????,?????????????????? & ????????????
				taskPluginCollector.collectDirtyRecord(record, e.getMessage());
			}
		}

		return record;
	}

	public static List<ColumnEntry> getListColumnEntry(
			Configuration configuration, final String path) {
		List<JSONObject> lists = configuration.getList(path, JSONObject.class);
		if (lists == null) {
			return null;
		}
		List<ColumnEntry> result = new ArrayList<ColumnEntry>();
		for (final JSONObject object : lists) {
			result.add(JSON.parseObject(object.toJSONString(),
					ColumnEntry.class));
		}
		return result;
	}

	private enum Type {
		STRING, LONG, BOOLEAN, DOUBLE, DATE, ;
	}

	/**
	 * check parameter:encoding, compress, filedDelimiter
	 * */
	public static void validateParameter(Configuration readerConfiguration) {

		// encoding check
		validateEncoding(readerConfiguration);

		//only support compress types
		validateCompress(readerConfiguration);

		//fieldDelimiter check
		validateFieldDelimiter(readerConfiguration);

		// column: 1. index type 2.value type 3.when type is Date, may have format
		validateColumn(readerConfiguration);

	}

	public static void validateEncoding(Configuration readerConfiguration) {
		// encoding check
		String encoding = readerConfiguration
				.getString(
						com.gaoding.datay.unstructuredstorage.reader.Key.ENCODING,
						com.gaoding.datay.unstructuredstorage.reader.Constant.DEFAULT_ENCODING);
		try {
			encoding = encoding.trim();
			readerConfiguration.set(Key.ENCODING, encoding);
			Charsets.toCharset(encoding);
		} catch (UnsupportedCharsetException uce) {
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("????????????????????????????????? : [%s]", encoding), uce);
		} catch (Exception e) {
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.CONFIG_INVALID_EXCEPTION,
					String.format("??????????????????, ???????????????: %s", e.getMessage()), e);
		}
	}

	public static void validateCompress(Configuration readerConfiguration) {
		String compress =readerConfiguration
				.getUnnecessaryValue(com.gaoding.datay.unstructuredstorage.reader.Key.COMPRESS,null,null);
		if(StringUtils.isNotBlank(compress)){
			compress = compress.toLowerCase().trim();
			boolean compressTag = "gzip".equals(compress) || "bzip2".equals(compress) || "zip".equals(compress)
					|| "lzo".equals(compress) || "lzo_deflate".equals(compress) || "hadoop-snappy".equals(compress)
					|| "framing-snappy".equals(compress);
			if (!compressTag) {
				throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
						String.format("????????? gzip, bzip2, zip, lzo, lzo_deflate, hadoop-snappy, framing-snappy " +
								"??????????????????, ???????????????????????????????????????: [%s]", compress));
			}
		}else{
			// ???????????????????????? compress:"",????????????,?????????compress?????????null
			compress = null;
		}
		readerConfiguration.set(com.gaoding.datay.unstructuredstorage.reader.Key.COMPRESS, compress);

	}

	public static void validateFieldDelimiter(Configuration readerConfiguration) {
		//fieldDelimiter check
		String delimiterInStr = readerConfiguration.getString(com.gaoding.datay.unstructuredstorage.reader.Key.FIELD_DELIMITER,null);
		if(null == delimiterInStr){
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.REQUIRED_VALUE,
					String.format("??????????????????????????????[%s]???????????????.",
							com.gaoding.datay.unstructuredstorage.reader.Key.FIELD_DELIMITER));
		}else if(1 != delimiterInStr.length()){
			// warn: if have, length must be one
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("???????????????????????????, ????????????????????? : [%s]", delimiterInStr));
		}
	}

	public static void validateColumn(Configuration readerConfiguration) {
		// column: 1. index type 2.value type 3.when type is Date, may have
		// format
		List<Configuration> columns = readerConfiguration
				.getListConfiguration(com.gaoding.datay.unstructuredstorage.reader.Key.COLUMN);
		if (null == columns || columns.size() == 0) {
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.REQUIRED_VALUE, "??????????????? columns");
		}
		// handle ["*"]
		if (null != columns && 1 == columns.size()) {
			String columnsInStr = columns.get(0).toString();
			if ("\"*\"".equals(columnsInStr) || "'*'".equals(columnsInStr)) {
				readerConfiguration.set(com.gaoding.datay.unstructuredstorage.reader.Key.COLUMN, null);
				columns = null;
			}
		}

		if (null != columns && columns.size() != 0) {
			for (Configuration eachColumnConf : columns) {
				eachColumnConf.getNecessaryValue(com.gaoding.datay.unstructuredstorage.reader.Key.TYPE,
						UnstructuredStorageReaderErrorCode.REQUIRED_VALUE);
				Integer columnIndex = eachColumnConf
						.getInt(com.gaoding.datay.unstructuredstorage.reader.Key.INDEX);
				String columnValue = eachColumnConf
						.getString(com.gaoding.datay.unstructuredstorage.reader.Key.VALUE);

				if (null == columnIndex && null == columnValue) {
					throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.NO_INDEX_VALUE,
							"??????????????????type, ????????????????????? index ??? value");
				}

				if (null != columnIndex && null != columnValue) {
					throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.MIXED_INDEX_VALUE,
							"??????????????????index, value, ???????????????????????????????????????");
				}
				if (null != columnIndex && columnIndex < 0) {
					throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
							String.format("index??????????????????0, ????????????index???[%s]", columnIndex));
				}
			}
		}
	}

	public static void validateCsvReaderConfig(Configuration readerConfiguration) {
		String  csvReaderConfig = readerConfiguration.getString(Key.CSV_READER_CONFIG);
		if(StringUtils.isNotBlank(csvReaderConfig)){
			try{
				UnstructuredStorageReaderUtil.csvReaderConfigMap = JSON.parseObject(csvReaderConfig, new TypeReference<HashMap<String, Object>>() {});
			}catch (Exception e) {
				LOG.info(String.format("WARN!!!!??????csvReaderConfig??????! ????????????,????????????????????????Map??????,??????????????????: %s", csvReaderConfig));
			}
		}
	}

	/**
	 *
	 * @Title: getRegexPathParent
	 * @Description: ???????????????????????????????????????
	 * @param @param regexPath
	 * @param @return
	 * @return String
	 * @throws
	 */
	public static String getRegexPathParent(String regexPath){
		int endMark;
		for (endMark = 0; endMark < regexPath.length(); endMark++) {
			if ('*' != regexPath.charAt(endMark) && '?' != regexPath.charAt(endMark)) {
				continue;
			} else {
				break;
			}
		}
		int lastDirSeparator = regexPath.substring(0, endMark).lastIndexOf(IOUtils.DIR_SEPARATOR);
		String parentPath  = regexPath.substring(0,lastDirSeparator + 1);

		return  parentPath;
	}
	/**
	 *
	 * @Title: getRegexPathParentPath
	 * @Description: ?????????????????????????????????????????????????????????????????????????????????????????????*???????.
	 * (API jcraft.jsch.ChannelSftp.ls(String path)????????????)  http://epaul.github.io/jsch-documentation/javadoc/
	 * @param @param regexPath
	 * @param @return
	 * @return String
	 * @throws
	 */
	public static String getRegexPathParentPath(String regexPath){
		int lastDirSeparator = regexPath.lastIndexOf(IOUtils.DIR_SEPARATOR);
		String parentPath = "";
		parentPath = regexPath.substring(0,lastDirSeparator + 1);
		if(parentPath.contains("*") || parentPath.contains("?")){
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("????????????path??????[%s]???????????????????????????????????????????????????????????????*???????", regexPath));
		}
		return parentPath;
	}

	public static void setCsvReaderConfig(CsvReader csvReader){
		if(null != UnstructuredStorageReaderUtil.csvReaderConfigMap && !UnstructuredStorageReaderUtil.csvReaderConfigMap.isEmpty()){
			try {
				BeanUtils.populate(csvReader,UnstructuredStorageReaderUtil.csvReaderConfigMap);
				LOG.info(String.format("csvReaderConfig????????????,?????????CsvReader:%s", JSON.toJSONString(csvReader)));
			} catch (Exception e) {
				LOG.info(String.format("WARN!!!!??????csvReaderConfig??????!??????BeanUtils.populate????????????csvReaderConfig????????????,??????????????????: %s;?????????????????????!CsvReader???????????????[%s]",
						JSON.toJSONString(UnstructuredStorageReaderUtil.csvReaderConfigMap),JSON.toJSONString(csvReader)));
			}
		}else {
			//????????????????????????, ??????10W???????????????
			csvReader.setSafetySwitch(false);
			LOG.info(String.format("CsvReader???????????????[%s],csvReaderConfig??????[%s]",JSON.toJSONString(csvReader),JSON.toJSONString(UnstructuredStorageReaderUtil.csvReaderConfigMap)));
		}
	}
}
