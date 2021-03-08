package com.gaoding.datay.common.model;

import java.util.HashMap;
/**
 * TODO
 * 扩展
 */
public class PluginConfs {
    
    private static HashMap<String,String> map = new HashMap<>();

    static{
        map.put("mysqlreader", "{\"name\":\"mysqlreader\",\"class\":\"com.gaoding.datay.mysqlreader.MysqlReader\",\"description\":\"useScene: prod. mechanism: Jdbc connection using the database, execute select sql, retrieve data from the ResultSet. warn: The more you know about the database, the less problems you encounter.\",\"developer\":\"yuanfang\"}");
        map.put("mysqlwriter", "{\"name\":\"mysqlwriter\",\"class\":\"com.gaoding.datay.mysqlwriter.MysqlWriter\",\"description\":\"useScene: prod. mechanism: Jdbc connection using the database, execute insert sql. warn: The more you know about the database, the less problems you encounter.\",\"developer\":\"yuanfang\"}");
        map.put("rocketmqwriter", "{\"name\":\"rocketmqwriter\",\"class\":\"com.gaoding.datay.rocketmqwriter.RocketMqWriter\",\"description\":\"数据写入RocketMQ\",\"developer\":\"yuanfang\"}");
        map.put("mongodbreader", "{\"name\":\"mongodbreader\",\"class\":\"com.gaoding.datay.mongodbreader.MongoDBReader\",\"description\":\"从mongoDB读取数据\",\"developer\":\"yuanfang\"}");

    }

    public static String get(String key){
        return map.get(key);
    }

}
