package com.gaoding.datay.core.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gaoding.datay.common.exception.DataXException;
import com.gaoding.datay.common.model.PluginConfs;
import com.gaoding.datay.common.util.Configuration;
import com.gaoding.datay.core.util.container.CoreConstant;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigParser {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigParser.class);

    /**
     * 指定Job配置路径，ConfigParser会解析Job、Plugin、Core全部信息，并以Configuration返回
     */
    public static Configuration parse(final String jobPath) {
        Configuration configuration = ConfigParser.parseJobConfig(jobPath);

        // merge dataY系统配置
        // configuration.merge(ConfigParser.parseCoreConfig(CoreConstant.DATAX_CONF_PATH),false);
        configuration.merge(ConfigParser.parseCoreDefaultConfig(
                "{\"entry\": {         \"jvm\": \"-Xms1G -Xmx1G\",         \"environment\": {}     },     \"common\": {         \"column\": {             \"datetimeFormat\": \"yyyy-MM-dd HH:mm:ss\",             \"timeFormat\": \"HH:mm:ss\",             \"dateFormat\": \"yyyy-MM-dd\",             \"extraFormats\":[\"yyyyMMdd\"],             \"timeZone\": \"GMT+8\",             \"encoding\": \"utf-8\"         }     },     \"core\": {         \"dataXServer\": {             \"address\": \"http://localhost:7001/api\",             \"timeout\": 10000,             \"reportDataxLog\": false,             \"reportPerfLog\": false         },         \"transport\": {             \"channel\": {                 \"class\": \"com.gaoding.datay.core.transport.channel.memory.MemoryChannel\",                 \"speed\": {                     \"byte\": -1,                     \"record\": -1                 },                 \"flowControlInterval\": 20,                 \"capacity\": 512,                 \"byteCapacity\": 67108864             },             \"exchanger\": {                 \"class\": \"com.plugin.BufferedRecordExchanger\",                 \"bufferSize\": 32             }         },         \"container\": {             \"job\": {                 \"reportInterval\": 10000             },             \"taskGroup\": {                 \"channel\": 5             },             \"trace\": {                 \"enable\": \"false\"             }          },         \"statistics\": {             \"collector\": {                 \"plugin\": {                     \"taskClass\": \"com.gaoding.datay.core.statistics.plugin.task.StdoutPluginCollector\",                     \"maxDirtyNumber\": 10                 }             }         }     } }"),
                false);

        // todo config优化，只捕获需要的plugin
        String readerPluginName = configuration.getString(CoreConstant.DATAX_JOB_CONTENT_READER_NAME);
        String writerPluginName = configuration.getString(CoreConstant.DATAX_JOB_CONTENT_WRITER_NAME);

        String preHandlerName = configuration.getString(CoreConstant.DATAX_JOB_PREHANDLER_PLUGINNAME);

        String postHandlerName = configuration.getString(CoreConstant.DATAX_JOB_POSTHANDLER_PLUGINNAME);

        Set<String> pluginList = new HashSet<String>();
        pluginList.add(readerPluginName);
        pluginList.add(writerPluginName);

        if (StringUtils.isNotEmpty(preHandlerName)) {
            pluginList.add(preHandlerName);
        }
        if (StringUtils.isNotEmpty(postHandlerName)) {
            pluginList.add(postHandlerName);
        }
        try {
            configuration.merge(parsePluginConfig(new ArrayList<String>(pluginList)), false);
        } catch (Exception e) {
            // 吞掉异常，保持log干净。这里message足够。
            LOG.warn(String.format("插件[%s,%s]加载失败，1s后重试... Exception:%s ", readerPluginName, writerPluginName,
                    e.getMessage()));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                //
            }
            configuration.merge(parsePluginConfig(new ArrayList<String>(pluginList)), false);
        }

        return configuration;
    }

    private static Configuration parseCoreConfig(final String path) {
        return Configuration.from(new File(path));
    }

    private static Configuration parseCoreDefaultConfig(final String content) {
        return Configuration.from(content);
    }

    public static Configuration parseJobConfig(final String path) {
        String jobContent = getJobContent(path);
        Configuration config = Configuration.from(jobContent);

        return SecretUtil.decryptSecretKey(config);
    }

    private static String getJobContent(String jobResource) {
        String jobContent;

        boolean isJobResourceFromHttp = jobResource.trim().toLowerCase().startsWith("http");

        if (isJobResourceFromHttp) {
            // 设置httpclient的 HTTP_TIMEOUT_INMILLIONSECONDS
            Configuration coreConfig = ConfigParser.parseCoreConfig(CoreConstant.DATAX_CONF_PATH);
            int httpTimeOutInMillionSeconds = coreConfig.getInt(CoreConstant.DATAX_CORE_DATAXSERVER_TIMEOUT, 5000);
            HttpClientUtil.setHttpTimeoutInMillionSeconds(httpTimeOutInMillionSeconds);

            HttpClientUtil httpClientUtil = new HttpClientUtil();
            try {
                URL url = new URL(jobResource);
                HttpGet httpGet = HttpClientUtil.getGetRequest();
                httpGet.setURI(url.toURI());

                jobContent = httpClientUtil.executeAndGetWithFailedRetry(httpGet, 1, 1000l);
            } catch (Exception e) {
                throw DataXException.asDataXException(FrameworkErrorCode.CONFIG_ERROR, "获取作业配置信息失败:" + jobResource, e);
            }
        } else {
            // jobResource 是本地文件绝对路径
            return jobResource;
            // try {
            //     jobContent = FileUtils.readFileToString(new File(jobResource));
            // } catch (IOException e) {
            //     throw DataXException.asDataXException(FrameworkErrorCode.CONFIG_ERROR, "获取作业配置信息失败:" + jobResource, e);
            // }
        }

        if (jobContent == null) {
            throw DataXException.asDataXException(FrameworkErrorCode.CONFIG_ERROR, "获取作业配置信息失败:" + jobResource);
        }
        return jobContent;
    }

    public static Configuration parsePluginConfig(List<String> wantPluginNames) {
        Configuration configuration = Configuration.newDefault();

        Set<String> replicaCheckPluginSet = new HashSet<String>();
        int complete = 0;

        //List<String> readerDirAsList = ConfigParser.getDirAsList(CoreConstant.DATAX_PLUGIN_READER_HOME);
        for (final String each : wantPluginNames) {
            Configuration eachReaderConfig = ConfigParser.parseOnePluginConfig(each, replicaCheckPluginSet);
            if (eachReaderConfig != null) {
                configuration.merge(eachReaderConfig, true);
                complete += 1;
            }
        }
        // List<String> writerDirAsList = ConfigParser.getDirAsList(CoreConstant.DATAX_PLUGIN_WRITER_HOME);
        // for (final String each : writerDirAsList) {
        //     Configuration eachWriterConfig = ConfigParser.parseOnePluginConfig(each, "writer", replicaCheckPluginSet,
        //             wantPluginNames);
        //     if (eachWriterConfig != null) {
        //         configuration.merge(eachWriterConfig, true);
        //         complete += 1;
        //     }
        // }

        if (wantPluginNames != null && wantPluginNames.size() > 0 && wantPluginNames.size() != complete) {
            throw DataXException.asDataXException(FrameworkErrorCode.PLUGIN_INIT_ERROR,
                    "插件加载失败，未完成指定插件加载:" + wantPluginNames);
        }

        return configuration;
    }

    public static Configuration parseOnePluginConfig(final String plugin, Set<String> pluginSet) {
        
        Configuration configuration = Configuration.from(PluginConfs.get(plugin));

        String pluginPath = configuration.getString("path");
        String pluginName = configuration.getString("name");
        if (!pluginSet.contains(pluginName)) {
            pluginSet.add(pluginName);
        } else {
            throw DataXException.asDataXException(FrameworkErrorCode.PLUGIN_INIT_ERROR, "插件加载失败,存在重复插件:" + plugin);
        }


        boolean isDefaultPath = StringUtils.isBlank(pluginPath);
        if (isDefaultPath) {
            // TODO  有用吗这个配置？
            //configuration.set("path", path);
        }

        Configuration result = Configuration.newDefault();

        result.set(String.format("plugin.%s", pluginName), configuration.getInternal());

        return result;
    }

    private static List<String> getDirAsList(String path) {
        List<String> result = new ArrayList<String>();

        String[] paths = new File(path).list();
        if (null == paths) {
            return result;
        }

        for (final String each : paths) {
            result.add(path + File.separator + each);
        }

        return result;
    }

}
