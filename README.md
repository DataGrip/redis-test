### Browser-based authentication

Azure log in  
`az login` or `az login --use-device-code`

Get access token (expired in an hour)

`az account get-access-token --scope https://redis.azure.com/.default > .envToken`

Find out your Object ID for the User principal

`az ad signed-in-user show --query id -o tsv`

### Test connection

Set your credentials (Azure Cache for Redis hostname and your personal Object ID) in `Main`  
  

    
   

