# Restaurant Stock Server

This project was created as part of interview assignment.

## Configuration

Endpoint interface and port can be configured via environment variables

`LISTEN_INTERFACE` - default value `localhost`

`LISTEN_PORT` - default value `8080`

## How to start

Run the application by executing:
```
sbt run
``` 

Run the testsuite by executing:
```
sbt test
```

## Error Handling

##### Removal of an item that is not in storage

Request will fail if a request to remove an item that is not in a storage is issued.

Status Code: *404 Not Found*

##### Update of an item that is not in storage

Request will fail if a request to update an item that is not in a storage is issued.

Status Code: *404 Not Found*

## Design Decisions

##### Data Storage
- The only implementation of `StockStorage` trait is `StockActorStorage` that is internally managing state via an actor. It is a clean way to manage state of the application.

