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

## SQL

> Even if the database will be mocked, please provide also the SQL that would be needed to create the tables as if we were using a real db.

In real life scenario where `StockActorStorage` would not be used, server should be backed by a transactional database. Based on the database used I would choose a schema evolution library (e.g. [liquibase](https://www.liquibase.org/)).

Example changelog for `liquibase`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
         http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd">

    <changeSet id="1" author="jiri.marsicek">
        <createTable tableName="stockitem">
            <column name="id" type="text">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="text">
                <constraints nullable="false"/>
            </column>
            <column name="quantity" type="decimal"/>
        </createTable>
    </changeSet>
</databaseChangeLog>
```
