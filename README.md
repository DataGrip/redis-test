## Connecting to Azure Redis Cache with Application

The token is obtained and updated automatically.

### Application in Azure

Create Azure application, e.g. `redis-test`. Grant access to Azure Redis Cache to this application.

### Test connection

Set the credentials for Azure app `redis-test` in `Main` and `RedisJDBCConnection`:

* Object ID for app
* Client ID for app
* Azure secret "Redis connection"  for app (Value)
* Tenant ID  