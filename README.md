Spring Boot Quartz example
==========

This repository contains Spring Boot (https://spring.io/projects/spring-boot) 
application example for cluster scheduling.

Requirements
----------
* Java 1.8 latest update installed
* Access to maven central
* Any external relational DB to simulate cluster scheduling

Features
----------
* Single job execution, on any application node, at given time
* Job history with retention policy
* Simple REST API to schedule jobs and retrieve job status

Building application
----------
Target creates packaged zip with executable jar (see build.gradle for exact location of zip package).
To change version edit gradle.properties. application.properties and log4j config are provided outside 
application jar for easy configuration. Application may be also configured through environment variables 
eg. this may be used with docker images. 

```bash
gradlew build
```