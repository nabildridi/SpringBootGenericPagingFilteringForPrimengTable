# Spring Boot generic paging, sorting and filtering for PrimeNg tables (V 6.0)

## Goal of the project

PrimeNg tables have a 'lazy' mode when displaying data, it sends all the requests of paging, sorting and filtering to the server to be processed.
The goal of the this project is to make this server side processing the most generic possible (Spring boot).

---

## Flow of work

It's super easy to work with it :

1. Collect the json sent by PrimeNg table in your controller
2. Send this json to the **SearchBuilder** class 
3. With the object returned from **SearchBuilder** call you dao 

---

## Structure of the project

- The 'ng' folder contains the sample front-end Angular project
- The Spring Boot project is a minimal showcase and can be used as a base for other projects, it contains : a sample entity, dao, controller and service and two core classes responsible of building queries

## How it works

The idea is to make an utility class that parse and convert a PrimeNg json request to a paging and sorting query and build an **RSQL** query for the columns and general filters, this rsql query will then converted to Jpa specification with **[rsql-jpa-specification](https://github.com/perplexhub/rsql-jpa-specification)** and executed against the dao.


## Core classes and initial setup

The two most important java classes in this project are :

- **org.nd.primeng.search.PrimengRequestData** : a bean to hold the data parsed from the PrimeNg table request
- **org.nd.primeng.search.SearchBuilder** : responsible for parsing the PrimeNg table json request, generating the paging and sorting jpa query and building an Rsql query from the filters
- Please refer to the class **org.nd.primeng.services.UserService** for an example to how to use those classes
- Your repository class needs to extends **JpaSpecificationExecutor<Class>**, please refer to UsersDao class for an example

This project uses **[rsql-jpa-specification](https://github.com/perplexhub/rsql-jpa-specification)** to work, please refer to its documentation to see how the intial setup is done.

## Two possible methods of use

- The simplest form : you can use the generated specification with your repository, example :

```java
return usersRepository.findAll((Specification<User>) queries.getSpec(), queries.getPageQuery());
```

- Advanced Method : if you need to add a condition to the RSQL query before execution you can use this form, example :

```java
customQuery = queries.getRsqlQuery() + " and name=='John'"
return usersRepository.findAll(RSQLJPASupport.<User>toSpecification(customQuery).and(RSQLJPASupport.toSort(queries.getSortQuery())), queries.getPageQuery());
```

## <font color="red">Very important notes about dates filtering</font>

In order to properly filter against date columns, you need to do two things :

- Never use java.util.Date or any other java date types as type in your entity classes, use only **java.time.LocalDateTime**
- You need to properly setup your timezone in the jvm with the parameter **-Duser.timezone**, example :

  -Duser.timezone=Europe/Paris

## Run the project

- Set your timezone in pom.xml in order to the date filtering to work properly :

```xml
<configuration>
	    <jvmArguments>
		 -Duser.timezone=Europe/Paris
	 </jvmArguments>
</configuration>
```

- Run the Spring boot project
- Run the Angular project


---

## Versions

### Changes in version 6.0 (20/12/2025)

- Maven Java version to 23
- Spring Boot to 3.4.0 version
- Angular to 19.0.0 version (Standalone components)
- Primeng to 19.0.1 version

### Changes in version 5.0 (23/01/2024)

- Maven Java version to 21
- Spring Boot to 3.2.2 version
- Angular to 17.0.0 version
- Primeng to 17.3.0 version

### Changes in version 4.3 (19/10/2022)

- Maven Java version to 18
- Removed the use of mysql from this demo application, instead it uses in memory H2 database now
- Sample data is in **resources/data.sql** file
- Spring Boot to 2.7.4 version
- Angular to 14.2.0 version
- Primeng to 14.1.2 version

### Changes in version 4.2.1 (16/08/2022)

- Processing of the boolean type filter
- Changes in the test database and the test java model

**[fix](https://github.com/nabildridi/SpringBootGenericPagingFilteringForPrimengTable/issues/31)**

### Changes in version 4.2 (19/06/2022)

- Angular to 13.3.0 version
- Primeng to 13.4.0 version

### Changes in version 4.1 (20/04/2021)

- Processing of the boolean type filter
- Changes in the test database and the test java model

### Changes in version 4.0

- Primeng 11 came with many changes in the table component : a date filter was added and the text filter can now have multiple rules, this version take account of those changes
- This project is compatible with the table component of both v10 and v11 of Primeng
