### Test connection with Redis driver  
  
`az login`  
choose your subscription  
  
`mvn install:install-file -Dfile=lib/redis-jdbc-driver-1.5.jar -DgroupId=com.jetbrains.datagrip -DartifactId=redis-jdbc-driver -Dversion=1.5 -Dpackaging=jar`  
`mvn clean compile`   
`mvn exec:java -Dexec.mainClass="RedisJdbc"`

### Test connection with Jedis  

`az login`   
choose your subscription  
`mvn clean`    
`mvn compile`    
`mvn exec:java -Dexec.mainClass="JedisTest"`
  
  
  
### Can be useful
`mvn dependency:copy-dependencies -DoutputDirectory=target/dependency`  
`mvn versions:display-dependency-updates`  

  

    
   

