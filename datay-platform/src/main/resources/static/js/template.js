var mysql2mysql_template = '{ \n' +
'   "job": {\n' +
'       "setting": {\n' +
'           "speed": {\n' +
'               "channel": 1\n' +
'           },\n' +
'           "errorLimit": {\n' +
'               "record": 0,\n' +
'               "percentage": 0.02\n' +
'           }\n' +
'       },\n' +
'       "content": [{\n' +
'           "reader": {\n' +
'               "name": "mysqlreader",\n' +
'               "parameter": {\n' +
'                   "username": "root",\n' +
'                   "password": "1234567",\n' +
'                   "connection": [{\n' +
'                       "querySql": [\n' +
'                           "select runoob_id,runoob_title,runoob_author,submission_date from runoob_tbl"\n' +
'                       ],\n' +
'                       "jdbcUrl": [\n' +
'                           "jdbc:mysql://localhost:3306/test"\n' +
'                           ]\n' +
'                       }]\n' +
'                   }\n' +
'               },\n' +
'           "writer": {\n' +
'               "name": "mysqlwriter",\n' +
'               "parameter": {\n' +
'                   "writeMode": "insert",\n' +
'                   "username": "root",\n' +
'                   "password": "1234567",\n' +
'                   "column": ["runoob_id", "runoob_title", "runoob_author", "submission_date"],\n' +
'                   "connection": [{\n' +
'                       "jdbcUrl": "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8",\n' +
'                       "table": ["runoob_tbl_0"]\n' +
'                   }]\n' +
'               }\n' +
'           }\n' +
'       }]\n' +
'   }\n' +
'}\n'



var mysql2mongodb_template = '{ \n' +
'   "job": {\n' +
'       "setting": {\n' +
'           "speed": {\n' +
'               "channel": 1\n' +
'           },\n' +
'           "errorLimit": {\n' +
'               "record": 0,\n' +
'               "percentage": 0.02\n' +
'           }\n' +
'       },\n' +
'       "content": [{\n' +
'           "reader": {\n' +
'               "name": "mysqlreader",\n' +
'               "parameter": {\n' +
'                   "username": "root",\n' +
'                   "password": "1234567",\n' +
'                   "connection": [{\n' +
'                       "querySql": [\n' +
'                           "select runoob_id,runoob_title,runoob_author,submission_date from runoob_tbl"\n' +
'                       ],\n' +
'                       "jdbcUrl": [\n' +
'                           "jdbc:mysql://localhost:3306/test"\n' +
'                           ]\n' +
'                       }]\n' +
'                   }\n' +
'               },\n' +
'           "writer": {\n' +
'               "name": "mongodbwriter",\n' +
'               "parameter": {\n' +
'                   "address": ["127.0.0.1:27017"],\n' +
'                   "userName": "root",\n' +
'                   "userPassword": "1234567",\n' +
'                   "dbName":"test",\n'+
'                   "collectionName":"test",\n'+
'                   "column": [{"name":"test","type":"string"}],\n' +
'                   "upsertInfo": {\n' +
'                       "isUpsert": "true",\n' +
'                       "upsertKey": "test"\n' +
'                   }\n' +
'               }\n' +
'           }\n' +
'       }]\n' +
'   }\n' +
'}\n'

var mysql2rocketmq = '{ \n' +
'   "job": {\n' +
'       "setting": {\n' +
'           "speed": {\n' +
'               "channel": 1\n' +
'           },\n' +
'           "errorLimit": {\n' +
'               "record": 0,\n' +
'               "percentage": 0.02\n' +
'           }\n' +
'       },\n' +
'       "content": [{\n' +
'           "reader": {\n' +
'               "name": "mysqlreader",\n' +
'               "parameter": {\n' +
'                   "username": "root",\n' +
'                   "password": "1234567",\n' +
'                   "connection": [{\n' +
'                       "querySql": [\n' +
'                           "select runoob_id,runoob_title,runoob_author,submission_date from runoob_tbl"\n' +
'                       ],\n' +
'                       "jdbcUrl": [\n' +
'                           "jdbc:mysql://localhost:3306/test"\n' +
'                           ]\n' +
'                       }]\n' +
'                   }\n' +
'           },\n' +
'           "writer": {\n' +
'               "name": "rocketmqwriter",\n' +
'               "parameter": {\n' +
'                   "ak": "asfasdfasdfasdfds",\n' +
'                   "sk": "asdfsdfsdfasdfasdf",\n' +
'                   "orderly": "false",\n' +
'                   "shardingKey":"test",\n'+
'                   "tag":"test",\n'+
'                   "topic": "test",\n' +
'                   "ns": "",\n' +
'                   "column":"test,test2,test3"\n'+
'               }\n' +
'           }\n' +
'       }]\n' +
'   }\n' +
'}\n'


var mongodb2rocketmq =  '{ \n' +
'   "job": {\n' +
'       "setting": {\n' +
'           "speed": {\n' +
'               "channel": 1\n' +
'           },\n' +
'           "errorLimit": {\n' +
'               "record": 0,\n' +
'               "percentage": 0.02\n' +
'           }\n' +
'       },\n' +
'       "content": [{\n' +
'           "reader": {\n' +
'               "name": "mongodbwriter",\n' +
'               "parameter": {\n' +
'                   "address": ["127.0.0.1:27017"],\n' +
'                   "userName": "root",\n' +
'                   "userPassword": "1234567",\n' +
'                   "dbName":"test",\n'+
'                   "collectionName":"test",\n'+
'                   "column": [{"name":"test","type":"string"}],\n' +
'                   "upsertInfo": {\n' +
'                       "isUpsert": "true",\n' +
'                       "upsertKey": "test"\n' +
'                   }\n' +
'               }\n' +
'           },\n' +
'           "writer": {\n' +
'               "name": "rocketmqwriter",\n' +
'               "parameter": {\n' +
'                   "ak": "asfasdfasdfasdfds",\n' +
'                   "sk": "asdfsdfsdfasdfasdf",\n' +
'                   "orderly": "false",\n' +
'                   "shardingKey":"test",\n'+
'                   "tag":"test",\n'+
'                   "topic": "test",\n' +
'                   "ns": "",\n' +
'                   "column":"test,test2,test3"\n'+
'               }\n' +
'           }\n' +
'       }]\n' +
'   }\n' +
'}\n'

var config_template = {
    'mysqltomysql':mysql2mysql_template,
    'mysqltomongodb':mysql2mongodb_template,
    'mysqltorocketmq':mysql2rocketmq,
    'mongodbtorocketmq':mongodb2rocketmq
}