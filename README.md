# DataY

集成的DataX的数据同步平台

- 标准springboot项目，不依赖DataX

- 目前支持Reader有 mysqlreader  mongodbreader    

- 目前支持的Writer有 mysqlwriter mongodbwriter rocketmqwriter 

- 练手项目

## 启动


### 配置文件
```
spring.datasource.url = jdbc:mysql://localhost/test?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true
spring.datasource.username = root
spring.datasource.password = 1234567
spring.jpa.show-sql = true
spring.jpa.database-platform = org.hibernate.dialect.MySQL5InnoDBDialect
spring.datasource.driver-class-name = com.mysql.jdbc.Driver
spring.jpa.generate-ddl = true
spring.jpa.hibernate.ddl-auto = update

# 日志路径
datay.log.home=/Users/yangwq/datay_logs
```

### build
```
mvn clean package -DskipTests
```

### 启动
```
cd datay-platform/target
java -jar datay-platform-1.0-SNAPSHOT.jar
http://localhost:8080
```

