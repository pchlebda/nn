# Account currency application

* The goal of this application is to register account and exchange money PLN/USD and USD/PLN.
* The application uses nbp api.

## Used technologies
* Java 21
* Maven 3.6.3
* Open API
* Spring-Boot
* Spring
* H2(file persistence to keep state)

## Usage

### Important commands
* Build application `mvn clean package -U`
* Run tests `mvn clean test`
* Start application `mvn spring-boot:run`

Application requires `./data` directory to store local database(`./data/account.mv.db`) with users account.
Please make sure that the directory exists and application has proper permissions.

The server start on default 8080 port. For local usage the expected endpoint is `http://localhost:8080`.


### Sample usage
* Register account. Please note that in response you get back `apiKey`. This is mandatory for subsequent queries. Pass it as a `x-api-key`.
```
curl --location 'http://localhost:8080/account' \
  --header 'Content-Type: application/json' \
  --data '{
  "firstName":"John",
  "lastName": "Smith",
  "balancePLN" : 100.00
  }'
```

* Get status. Please note that you will need to pass `x-api-key` mandatory http header. Take it from previous step.
```
curl --location --request GET 'http://localhost:8080/account' \
--header 'x-api-key: 407b11ff-c806-4ad3-bc4a-2d16873478dd' \
--header 'Content-Type: application/json' \
--data '{
    "firstName":"John",
    "lastName": "Smith",
    "balancePLN" : 100
}'
```

* Exchange currency 
```
curl --location 'http://localhost:8080/account/exchange' \
--header 'x-api-key: 407b11ff-c806-4ad3-bc4a-2d16873478dd' \
--header 'Content-Type: application/json' \
--data '{
    "from":"PLN",
    "to": "USD",
    "amount" : 10
}'
```

For more info regarding API please check [open API](openapi/account_api.yaml).