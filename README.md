### Status
[![Build Status](https://travis-ci.org/olavloite/spanner-jdbc.svg?branch=master)](https://travis-ci.org/olavloite/spanner-jdbc)
[![Quality Gate](https://sonarcloud.io/api/badges/gate?key=nl.topicus%3Aspanner-jdbc&metric=coverage)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Coverage](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=coverage)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Reliability Rating](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=reliability_rating)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Security Rating](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=security_rating)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Maintainability Rating](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=sqale_rating)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)


# spanner-jdbc
JDBC Driver for Google Cloud Spanner

An open source JDBC Driver for Google Cloud Spanner, the horizontally scalable, globally consistent, relational database service from Google. The JDBC Driver that is supplied by Google is quite limited, as it does not allow any inserts, updates or deletes, nor does it allow DDL-statements.

This driver supports a number of unsupported features of the official JDBC driver:
* DML-statements (INSERT, UPDATE, DELETE)
* DDL-statements (CREATE TABLE [IF NOT EXISTS], ALTER TABLE, CREATE INDEX [IF NOT EXISTS], DROP TABLE [IF EXISTS], ...)
* Transactions (both read/write and read-only)

The driver ofcourse also supports normal SELECT-statements, including parameters. Example usage and tutorials can be found on http://www.googlecloudspanner.com/.

Releases are available on Maven Central and here: https://github.com/olavloite/spanner-jdbc/releases. Current release is version 1.0.3.

Include the following if you want the thick jar version that includes all (shaded) dependencies. This is the recommended version unless you know that the transitive dependencies of the small jar will not conflict with the rest of your project.

<div class="highlight highlight-text-xml"><pre>
&lt;<span class="pl-ent">dependency</span>&gt;
 	&lt;<span class="pl-ent">groupId</span>&gt;nl.topicus&lt;/<span class="pl-ent">groupId</span>&gt;
    	&lt;<span class="pl-ent">artifactId</span>&gt;spanner-jdbc&lt;/<span class="pl-ent">artifactId</span>&gt;
    	&lt;<span class="pl-ent">version</span>&gt;1.0.3&lt;/<span class="pl-ent">version</span>&gt;
&lt;/<span class="pl-ent">dependency</span>&gt;
</pre></div>

You can also use the driver with third-party tools such as SQuirreL, SQL Workbench, DbVisualizer, DBeaver or Safe FME. Have a look at this site for more information on how to use the driver with different tools and frameworks: http://www.googlecloudspanner.com/

Downloads for both the current and older versions can be found here: https://github.com/olavloite/spanner-jdbc/releases

## Data Manipulation Language (Insert/Update/Delete)
This driver does allow DML operations, but with some limitations because of the underlying limitations of Google Cloud Spanner. Google Cloud Spanner essentially limits all data manipulation to operations that operate on one record. This driver circumvents that by translating statements operating on multiple rows into SELECT-statements that fetch the records to be updated, and then updates each row at a time in one transaction. Data manipulation operations that operate on one row at a time are recognized by the driver and sent directly to the database. This means that normal data manipulation generated by frameworks like Hibernate are executed without any additional delays.

Please note that the underlying limitations of Google Cloud Spanner transactions still apply: https://cloud.google.com/spanner/quotas. This means a maximum of 20,000 mutations and 100MB of data in one transaction. You can get the driver to automatically bypass these quotas by setting the connection property AllowExtendedMode=true (see the Wiki-pages of this driver: https://github.com/olavloite/spanner-jdbc/wiki/URL-and-Connection-Properties).

Example of bulk INSERT:  
```sql
INSERT INTO TABLE1  
(COL1, COL2, COL3)  
SELECT SOMECOL1, SOMECOL2, SOMECOL3  
FROM TABLE2  
WHERE SOMECOL1>? AND SOMECOL3 LIKE ?  
```

Example of bulk INSERT-OR-UPDATE:  
```sql
INSERT INTO TABLE1  
(COL1, COL2, COL3)  
SELECT COL1, COL2+COL4, COL3*2  
FROM TABLE1  
WHERE COL4=?  
ON DUPLICATE KEY UPDATE  
```

The above UPDATE example is equal to:
```sql
UPDATE TABLE1 SET COL2=COL2+COL4 AND COL3=COL3*2 WHERE COL4=?
```
(assuming that COL1 is the primary key of the table).

Example of bulk UPDATE:  
```sql
UPDATE TABLE1 SET  
COL1=COL1*1.1,  
COL2=COL3+COL4  
WHERE COL5<1000  
```

Have a look at this article for more DML examples: http://www.googlecloudspanner.com/2018/02/data-manipulation-language-with-google.html

## JPA and Hibernate
The driver is designed to work with applications using JPA/Hibernate. See https://github.com/olavloite/spanner-hibernate for a Hibernate Dialect implementation for Google Cloud Spanner that works together with this JDBC Driver.

A simple example project using Spring Boot + JPA + Hibernate + this JDBC Driver can be found here: https://github.com/olavloite/spanner-jpa-example

Example usage:

```java
spring.datasource.driver-class-name=nl.topicus.jdbc.CloudSpannerDriver

spring.datasource.url=jdbc:cloudspanner://localhost;Project=projectId;Instance=instanceId;Database=databaseName;SimulateProductName=PostgreSQL;PvtKeyPath=key_file;AllowExtendedMode=false
```

The last two properties (SimulateProductName and PvtKeyPath) are optional.
All properties can also be supplied in a Properties object instead of in the URL.

You either need to
* Create an environment variable GOOGLE_APPLICATION_CREDENTIALS that points to a credentials file for a Google Cloud Spanner project.
* OR Supply the parameter PvtKeyPath that points to a file containing the credentials to use.

The server name (in the example above: localhost) is ignored by the driver, but as it is a mandatory part of a JDBC URL it needs to be specified.
The property 'SimulateProductName' indicates what database name should be returned by the method DatabaseMetaData.getDatabaseProductName(). This can be used in combination with for example Spring Batch. Spring Batch automatically generates a schema for batch jobs, parameters etc., but does so only if it recognizes the underlying database. Supplying PostgreSQL as a value for this parameter, ensures the correct schema generation.

## Distributed transactions
As of version 0.20 and newer the driver has support for distributed transactions (XADatasource and XAResource). Note that this is not a feature that is supported by Google Cloud Spanner, and that the driver needs to simulate support for two-phase commit by storing all prepared mutations in a custom table. Have a look here for a sample project: https://github.com/olavloite/spring-boot/tree/master/spring-boot-samples/spring-boot-sample-jta-atomikos

## Spring Boot
The driver has been tested with a number of popular frameworks. Have a look at this page for a list of sample applications with Spring Boot and related frameworks: http://www.googlecloudspanner.com/2017/12/google-cloud-spanner-and-spring-boot.html

### Credits
This application uses Open Source components. You can find the source code of their open source projects along with license information below.

A special thanks to Tobias for his great JSqlParser library.
Project: JSqlParser https://github.com/JSQLParser/JSqlParser 
Copyright (C) 2004 - 2017 JSQLParser Tobias
