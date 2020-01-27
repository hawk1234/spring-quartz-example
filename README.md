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

Theory
----------
#### 1. Important classes
What are the main classes that the programmer wil work with

* *Scheduler* - Maintains registry of *JobDetail*-s and *Trigger*-s. It is main interface by which user can 
get information about existing jobs and schedule new jobs for execution.
* *Job* - Interface that must be implemented by user for *Quartz* to be able to schedule and execute the job. 
**Implementation must have public no argument constructor**
* *JobDataMap* - Structure that holds *Job* parameters as key-value pairs
* *JobDetail* - Defines what should be done when the job is triggered. You obtain it from *JobBuilder*. 
Configuration available when creating *JobDetail* is listed below.

Information configurable through *JobBuilder*:

Property | Description | Required | Default Value
-------- | ----------- | -------- | -------------
job class | Class of implemented *Job* that will be instantiated during job execution | yes | N/A
key | Unique job identifier - *JobKey* - consists of job name and group | yes | N/A
job data | Parameters (key, value) with which job will be executed. **Parameters will be available for every job execution**. Represented by *JobDataMap* | no | empty *JobDataMap*
durable | Indicates that job should be kept in underlying *JobStore* when there are no *Trigger*-s associated with it | no | false
request recovery | Informs *Quartz* whether the job should be re-executed when it wasn't finished successfully **as a result of JVM shutdown** | no | false

Other configuration:

Property | Description | Required | Default Value | How to configure
-------- | ----------- | -------- | ------------- | ----------------
update data | Indicates that *JobDataMap* of *JobDetail* should be re-persisted after job execution | no | false | annotate *Job* with *PersistJobDataAfterExecution*
non concurrent | Indicate whether job can't be executed on multiple nodes within cluster | no | false | annotate *Job* with *DisallowConcurrentExecution*
execute in JTA transaction | Indicate whether job should be executed in JTA transaction. **NOTE**: This is not *JobDetail* specific configuration it can be also set on scheduler level with property `org.quartz.scheduler.wrapJobExecutionInUserTransaction`. | no | false | annotate *Job* with *ExecuteInJTATransaction*

* *Trigger* - Defines when job should be executed. You obtain it from *TriggerBuilder*. 
Key information to define when creating *Trigger* are listed below. **NOTE:** not all configuration options are covered.

Property | Description | Required | Default Value
-------- | ----------- | -------- | -------------
job | *JobKey* of the job that should be executed | yes | N/A
key | Unique trigger identifier - *TriggerKey* - consists of trigger name and group | yes | N/A
schedule | Determines how often job should be triggered eg. *SimpleScheduleBuilder* or *CronScheduleBuilder* | no | job will be triggered once
start at | Determines lower boundary before which job can't be triggered | no | the time at which trigger was created
end at | Determines upper boundary after which job can't be triggered | no | trigger has no upper boundary
job data | Parameters (key, value) with which job will be executed. **Parameters will be available for job execution triggered by this trigger**. Represented by *JobDataMap* | no | empty *JobDataMap*
priority | If multiple triggers have the same fire time, determines the order of triggering. Highest priority first | no | Quartz default - 5
misfire instruction | Determines how trigger should behave when it missed its fire time. **NOTE:** this property is defined on schedule builder level, but it is a *Trigger* property | no | *Trigger#MISFIRE_INSTRUCTION_SMART_POLICY* 

* *JobExecutionContext* - passed to *Job* implementation containing references to *JobDetail* and *Trigger* that 
invoked the execution. **Allows to retrieve merged *JobDataMap* that contains parameters from both *JobDetail* and
*Trigger***. *Trigger* parameters override *JobDetail* parameters with the same names.
* *JobExecutionException* - special exception that can be thrown by *Job* to indicated that *Job* 
should re-fire immediately or that *Trigger* associated with the job should be unscheduled. **NOTE:** For what
I tested in such situation *Job* is being executed constantly on the same node when clustering is used.

#### 2. More terminology
* Fail-over - occurs when one of the nodes fails while in the midst of executing one or more jobs. 
When a node fails, the other nodes detect the condition and identify the jobs in the database that 
were in progress within the failed node. Any jobs marked for recovery (with the “requests recovery” 
property on the JobDetail) will be re-executed by the remaining nodes. Jobs not marked for recovery 
will simply be freed up for execution at the next time a related trigger fires.
* Misfire - is a situation when *Trigger* wasn't able to fire in its designated time eg. application was down.
Default value of misfire property is *Trigger#MISFIRE_INSTRUCTION_SMART_POLICY*, behavior depends on the schedule 
that was used eg. *Trigger* that was scheduled to execute once will fire immediately and *Trigger* that was scheduled
to fire infinitely with given time interval will skip such execution. Article that describes possible misfire
instructions: https://dzone.com/articles/quartz-scheduler-misfire

#### 3. Quartz groups

Feature that helps organize tasks into logical groups (*JobDetail*-s and *Trigger*-s). Certain operations 
can be performed on the whole group by using *GroupMatcher* of jobs eg. pause or resume.

#### 4. JDBC *JobStore*
Currently *Quartz* supports two *JobStore* types. Default is in memory job store. JDBC job store is persistent type 
of store. It is only *JobStore* that supports clustering. Multiple schedulers (**not clustered**) can run against 
same JDBC job store, each with its unique scheduler name.

There are two types JDBC job store to choose from *JobStoreTX* and *JobStoreCMT*. Transactional behaviour depends on
the type of *JobStore* you choose. **NOTE:** It only refers to operations regarding job scheduling not job execution 
itself. It's not related to `org.quartz.scheduler.wrapJobExecutionInUserTransaction` property.

ref. http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigJobStoreTX.html<br/>
ref. http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigJobStoreCMT.html

##### 4.1. Configuration with vanilla *Quartz*
In vanilla *Quartz* minimum following properties need to be configured for JDBC *JobStore*

```properties
org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.dataSource=quartzDataSource
``` 

##### 4.2. Configuration with *Spring*
With *Spring* we configure single property

```properties
# In spring by default it is org.springframework.scheduling.quartz.LocalDataSourceJobStore (JobStoreCMT variant) 
spring.quartz.job-store-type=jdbc
```

If no *DataSource* will be assigned to *Quartz*, it will use *Spring* default *DataSource*. 
You can use `org.springframework.boot.autoconfigure.quartz.QuartzDataSource` to create a dedicated 
*Quartz* *DataSource* see `com.mz.example.config.DBConfiguration`

ref. https://www.baeldung.com/spring-quartz-schedule#2-jdbc-jobstore

When the jdbc store is used, the schema can be initialized on startup:
```properties
#available values:
#   always - Always initialize datasource
#   embedded - Only initialize embedded datasource - default value
#   never - Do not initialize datasource
spring.quartz.jdbc.initialize-schema=never
```

ref. `org.springframework.boot.jdbc.DataSourceInitializationMode`

Scripts for *JobStore* initialization for different databases can be found here:
https://github.com/quartz-scheduler/quartz/tree/quartz-2.3.1/quartz-core/src/main/resources/org/quartz/impl/jdbcjobstore

More reading: https://github.com/quartz-scheduler/quartz/wiki/How-to-Setup-Databases

#### 5. Running in cluster
* Allows to synchronize multiple instances of *Scheduler* class
* Works only with the JDBC *JobStore*. Synchronization is done on database level
* Each application instance within the cluster needs to have unique id. 
Can be easily achieved with `AUTO` id assignment.

Minimum required configuration (with spring):
```properties
#Define that multiple instances of this application will use the same data store. Spring configuration requires spring.quartz.properties. prefix
spring.quartz.properties.org.quartz.jobStore.isClustered=true
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
```
ref. http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigJDBCJobStoreClustering.html

#### 6. Integration With Spring
* Use `spring-boot-starter-quartz`
* Manages *Scheduler* bean for the user
* **Allows to inject other beans into our *Job* implementation with the introduction of *QuartzJobBean* - base *Job* 
implementation that should be extended by user**
* Exposes quartz properties with `spring.quartz.properties` prefix
* Easy JDBC *DataSource* configuration - see [4.2](#42-configuration-with-spring)