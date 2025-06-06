### Test connection with Redis driver  

`mvn dependency:copy-dependencies -DoutputDirectory=target/dependency`  

`java -cp "target/classes:lib/redis-jdbc.jar:target/dependency/*" RedisJdbcMethod`
  
or  
  
`mvn install:install-file -Dfile=lib/redis-jdbc.jar -DgroupId=redis-jdbc -DartifactId=redis-jdbc -Dversion=1.0 -Dpackaging=jar`  
    
   
### Test connection with Jedis
`mvn clean`    
`mvn compile`    
`mvn exec:java -Dexec.mainClass="RedisNoClean2"`    
