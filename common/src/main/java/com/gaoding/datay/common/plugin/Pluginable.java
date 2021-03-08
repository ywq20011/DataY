package com.gaoding.datay.common.plugin;

import com.gaoding.datay.common.util.Configuration;

public interface Pluginable {
	/**
	 * 开发者
	 * @return
	 */
	String getDeveloper();

	/**
	 * 插件描述
	 * @return
	 */
    String getDescription();

    /**
     * 设置插件配置
     * @param pluginConf
     */
    void setPluginConf(Configuration pluginConf);

    /**
     * 初始化插件
     */
	void init();

	/**
	 * 销毁插件
	 */
	void destroy();

	/**
	 * 获取插件名称
	 * @return
	 */
    String getPluginName();

    /**
     * 
     * @return
     */
    Configuration getPluginJobConf();

    Configuration getPeerPluginJobConf();

    public String getPeerPluginName();

    void setPluginJobConf(Configuration jobConf);

    void setPeerPluginJobConf(Configuration peerPluginJobConf);

    public void setPeerPluginName(String peerPluginName);

}
