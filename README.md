# Big Data: Foreign Key Constraint Aware, Backup/Restore Application Integration

## Overview
The purpose of the application is to centralize big data import/export jobs, with foreign key constraint awareness for multiple applications. A web user interface that can easily be integrated into existing other web applications using CORS is provided.
A proxy server mediates spring xd processes through REST API interactions, setting up  and keeping in sync  job definitions and  associated  stream tap  definitions  automatically for registered data sources.

The application consists of 3 modules

1. forgeignkey-constraint-walker-proxy-server
2. spring-xd-constraint-aware-jdbc-export-job-module
3. spring-xd-constraint-aware-jdbc-import-job-module

## Backup Web User Interface
This allows selection of a table or group of tables. Which can optionally be filtered by date range. On 'validate' it then walks the relational table parent hierarchy, using relevant foreign key result set values, suggesting all the parents of the selected table(s). Each table suggested is backed by a sql query. The total list of sql queries is then consolidated by table. The user can override the suggestions removing parents they do not wish to include in the backup. On 'start' the sql queries are ordered and parsed to a new spring xd job instance. Which writes, in parallel, each table export to a file where each line is a json object representing a single table record (null fields are represented as empty json strings – which simplifies mapping during later restore/import). Naming the file by table name prefixed by the order it should be loaded in. The resulting files are compressed. Each job instance has a alphanumeric key which is used to name the backup archive as well. Which can then be made available for download.

## Relational Dependency Delete Web User Interface
This service has a similar user interface. On 'validate' it walks the relational table child hierarchy using relevant foreign key result set values, it suggests all the children of the selected table. Each child table suggested is backed by a sql query. The user can override the suggestions removing children they do not wish to include in the delete. On 'start' the sql queries are ordered with most distant child first and then executed in order.


## Restore Web User Interface
The service has a very simplistic interface and allows the upload of the zip archive containing the json files to be restored/imported to that database associated with the javascript var " xdjobdefname " (is explained in more detail bellow) The name of the archive is arbitrary, a new alphanumeric key generated and used to keep track of the restore job.


When the user clicking start a progress indicator is displayed until the proxy has received confirmation that the spring xd job has completed. A report summary is then displayed showing any errors or warning.

## Integration with Your Web application
All three user interfaces are plain html files which should be severed by your web application as static content.

In **backup.html**, **restore.html** and **delete.html** the host url for 'forgeignkey-constraint-walker-proxy-server' needs to be set as shown

The 'forgeignkey-constraint-walker-proxy-server' uses the configuration class  *com.fluidnotions.server.SimpleCORSFilter* to enable Cross Origin Requests.
If running proxy server on the same host as  your web application an alternative port can be set in application.properties > port: .

### Relational Dependency Delete Implementation
Since this service is just about executing the sequence of delete statements on the database is the correct order it is handled from within 'forgeignkey-constraint-walker-proxy-server'. The statements are simple executed with most distant child first.

### Integration with Spring XD
We should already have a spring xd server set up (refer to the  pring xd  project documentation).
For the these services  we are simply adding  two  new custom job modules *'jdbcexport'* and *'jdbcimport'*.

The way the integration works is job definitions and associated stream taps  created for each data source. The whole point of using xd is to centralize data processing. You may have a definition for each your web application instances, which contains the details for it's data source. The base job definition identifier name therefore also needs to be set in  **backup.html**, **restore.html**  and  **delete.html**

In the forgeignkey-constraint-walker-proxy-server, we have a table named *MY_XD_JOB_MOD_DS* in the xdjob database (this is the same database that our spring xd runtime is running on)

This table is where we set up a record for each tenant database on all your web application instances. The idea of the multi- tenant version of the proxy server, is that we only have one proxy server instance running on the same server as the spring xd runtime. There is no interface for adding *MY_XD_JOB_MOD_DS* records because the most efficient way would be to use mysql *LOAD DATA INFILE* and load a total list from a *CSV*.

The fields *EXPORT_DIR* and *IMPORT_DIR* act as indicators to the proxy server when it automatically creates jobs and tap streams (which act as a way to monitor progress) on start up. If either is null a job/streams definition (used as a template for job instances) will not be created. In the case of both containing directory paths on the xd server a total of 4 definitions will be created and deployed (if they have changed or don't already exist).

For example if the *JOB_DEF_NAME*  is 'tenant1' and both *EXPORT_DIR* and *IMPORT_DIR* the following definitions will be created and deployed
(i) tenant1import
(ii) tenant1importtap
(iii) tenant1export
(iv) tenant1exporttap.
While in a case where *EXPORT_DIR* is null only (i) and (ii) will be created. Example of records used during test can be found in *forgeignkey-constraint-walker-proxy-server/src/main/resources/MY_XD_JOB_MOD_DS-example.sql*

On proxy server start up when the singleton bean *com.fluidnotions.server.walker.database.DatabasesMetaDataMap* is created the method *initStaticDatasourceMap()* is executed this looks up all *MY_XD_JOB_MOD_DS* table records and runs the schema analyzer on each database and builds a *com.fluidnotions.server.walker.database.DatabaseDetails*  object for each record which is placed in a static map for use by the proxy server, while servicing requests from multiple web application instances each which hosts a page with the appropriate 'xdjobdefname' set to the desired target definition id.

> **Note**: To enable multi-tenant capabilities on a specific web application instance all you need to do is set the 'xdjobdefname' for the active tenant. Or have a list of ids a particular tenant can choose from representing import/export data sources. Or you could just associate your 'xdjobdefname's' with the tenant key in use, since these are already available in the page context and can easily be used to set the 'xdjobdefname' js variable. (By copying the  delete.html, restore.html and backup.html files into FTL's and then referencing the tenant key as the set value for the 'xdjobdefname' js variable)

The *com.fluidnotions.server.walker.database.DatabaseDetails* object also contains methods to construct spring xd job definitions and their associated stream tap definitions. After the object has been saved in the static singleton map the method *com.fluidnotions.server.walker.XdDefinitionSetup.checkAllXdDefSetup(Map)* is executed this then checks that all the correct definitions for jobs and streams exist on the spring xd runtime, via REST API calls, if it finds any that do not exist it will create them automatically. It also checks that the spring xd runtime definitions match the data in the *MY_XD_JOB_MOD_DS* table records and we'll delete and recreate definitions that do not match.

### How to fix issues with spring xd definition deployment statues, if they arise
Though syncing works correctly deleting and then recreating a definitions can sometime cause issues with zookeeper data nodes. All spring xd definition are persisted not in the xdjob database but on the zookeeper datanode. Though the proxy builds definitions automatically it may still be necessary to occasionally remove faulty definition nodes on zookeeper. These show up in spring xd admin-ui as status 'deploying' that never change to 'deployed'. The proxy server should be stopped. The spring xd runtime should be stopped. Individual definition nodes can be removed using  *./zkCli.sh -server 127.0.0.1:2181*  the zookeper CLI detailed instructions on how to remove nodes can be found on the project's help pages.

> eg: with the command:  *rmr /xd* to remove the whole tree recursively which might not be a good idea if you have hundreds of definitions. The spring xd runtime should then be started. Then when the proxy server is started it will discover the now missing definition of the faulty definition node you just removed and create it.

When a list of queries is sent to the '/backup/{xdJobDefName}' POST the proxy saves them to a table called *MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM* which is created in the xdjob database.

The create statement for the *MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM* table can be found in the project source  *src/main/resources/init.sql* file. The proxy creates an alphanumeric key which is saved into   *MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM* column *JOB_KEY* and this servers as a grouping for all the table queries for the particular job run. *(These queries are too long to be parsed as job parameters which is the reason for this extra table)* The proxy then launches a job instance of the already configured job definition base name suffixed by either "import" or "export", specified by the name provided parsing in the  alphanumeric job key as a job parameter.

The xd job module 'jdbcexport' contains a class *com.fluidnotions.springbatch.xml.export.extended.QueryListPartitioner* which uses the job key to get the list of queries for the export job. It then spawns a thread to write the results of each table query to a file where each line is a json object representing a table row, naming it appropriately then compresses all the files as a zip archive named with the alphanumeric job key which was returned to the client with the initial POST.

## The Backup Process
The custom xd job module was developed as an independent spring batch project 'spring-xd-constraint-aware-jdbc-export-job-module' it was then packaged as an xd job module. The packaging can be found in the source repository under   *spring-xd-constraint-aware-jdbc-export-job-module/xd/modules/job/jdbcexport*.

The jar file **jdbcexport.jar** located in  *spring-xd-constraint-aware-jdbc-export-job-module/xd/modules/job/jdbcexport/lib/* is just the jarred contents of the project *spring-xd-constraint-aware-jdbc-export-job-module /target/classes/*folder.

### Backup XD Module Setup
The jdbcexport folder then needs to be included in the  *xd-customized-singlenode-setup/spring-xd-1.0.2.RELEASE/xd/modules/job* folder (see project documentation for set up and running of xd-customized-singlenode-setup). On xd restart the job module will then be available from which definitions are created and deployed automatically by the proxy server on start up, ready for use, when launching job instances.

Lastly the **xdserverurl** property, needs to be provided in the 'forgeignkey-constraint-walker-proxy-server'  **application.properties** so the proxy knows where to send the job launch requests to.

### Downloading the Backup Archive
Since the backup job is running on spring xd and the proxy server is mediating the interaction between the browser client and spring xd, we need a way for the proxy server to know when a particular job has completed. The POST to launch the job on xd does not respond with the job execution id. But there is a workaround.

We create a new table called *MY_XD_JOB_EXECUTION_STATUS_PAYLOAD* in the xdjob database *(the one that spring xd uses, where we created the *MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM* table)*

A tap stream is created by the proxy server on start up  for each of our job definitions, an example is shown bellow. The easiest place to  manually  create simple taps like this is in the xd shell,though this is not necessary since the proxy server handles creation automatically . This simple tap will then capture JobExecution events, related to any job launched using the job definition specified and save them to the tables single column 'payload'.

*stream create --name  tenant1 exporttap --definition "tap:job: tenant1 export.job > transform | jdbc --tableName= MY_ XD_JOB_EXECUTION_STATUS_PAYLOAD --url=jdbc:mysql://localhost/ datasource1  --username=root --password=mysql --driverClassName=com.mysql.jdbc.Driver" --deploy*

Here we are just using the default xd transform which simply converts the JobExecution tap payload to a string, before piping it to a jdbc sink. It is possible to use a custom table for our sink and provide a transform to map fields of the JobExecution to our custom table. This will enable more efficient look up. But the default transform is good enough for our purposes at this point.

The proxy server can then poll the table waiting for a payload to be inserted with a completed status. We use the unique jobKey to identify the correct table row for our launched job.

*SELECT * FROM  MY_ XD_JOB_EXECUTION_STATUS_PAYLOAD where payload like '%jobKey=6iK0q%';*

The class *com.fluidnotions.server.walker.database.CheckXdJobStatus*  has been added for this purpose. It polls the datasource with the query every second for 60 seconds *(the default timeout which can be changed in application.properties)* trying to find the 'status=COMPLETED' string in the JobExecution payload. This process is triggered within the controller method mapped to '/backup/{xdJobDefName}' so that the response to the browser client request contains a result field with either completed status or an error condition.

In the browser client a download button then becomes available, to allow the user to download the backup file or in the case of an error an alert will show. In the proxy server application.properties the downloadUrlBase  property needs to be set which provides the url presumably on the same server where xd is running where the compressed backup files can be downloaded from. (eg: http://localhost:9090/downloads/) This value should share a base path with the *MY_XD_JOB_MOD_DS* record field *EXPORT_DIR*.

## The Restore Process
It should be noted that restore jobs are much slower then backup jobs. They are executed sequentially and the custom completion policy for the job uses a dynamic commit interval so that it will commit either at the end of the file ie: group of records from a single table or after 1000 records (default) in the case of many small files the result is a very small commit interval which means the job takes longer. The  dynamic commit interval is however necessary because we are dealing with foreign key constraints and loading order. There is a  xdjobcompletetimeout  property in application properties that is used in both backup and restore jobs which may need to be increased.
The *com.fluidnotions.springbatch.iimport.extended.TupleJdbcBatchItemWriter* class contains sophisticated second pass logic that will retry inserts that fail because of foreign key constraint violations. Only if foreign key constraint violations are detected on second pass does the report show an error and this has never occurred in testing. Failed inserts show in the report as warnings and are usually due to the fact that the record already exists in the target database. Since this is a very common occurrence for certain tables like _TYPE tables as seen in the report bellow  TupleJdbcBatchItemWriter  uses sql 'insert ignore' by default more details about what is ignored in code.

### Restore XD Module Setup
The custom xd job module was developed as an independent spring batch project 'spring-xd-constraint-aware-jdbc-import-job-module' it was then packaged as an xd job module. The packaging can be found in the source repository under  spring-xd-constraint-aware-jdbc-import-job-module/xd/modules/job/jdbcimport.

The jar file **jdbcimport.jar** located in *spring-xd-constraint-aware-jdbc-import-job-module/xd/modules/job/jdbcimport/lib/* is just the jarred contents of the project *spring-xd-constraint-aware-jdbc-import-job-module/target/classes/* folder.

The jdbcxmlexport folder then needs to be included in the  *xd-customized-singlenode-setup/spring-xd-1.0.2.RELEASE/xd/modules/job* folder (see File Generation documentation for set up and running of xd-customized-singlenode-setup). On xd restart the job module will then be available from which definitions are created and deployed automatically by the proxy server on start up, ready for use, when launching job instances.

Lastly the 'xdserverurl' property, needs to be provided in the 'forgeignkey-constraint-walker-proxy-server' **application.properties** so the proxy knows where to send the job launch requests to.

### Uploading the Backup Archive for Restore
Files uploaded for restore jobs end up in the folder path specified in the proxy server's application.properties file for property *uploadUrlDirectoryBase*. The first step of the restore job is 'step1-unzipFile' which then decompresses the archive to the directory specified in *IMPORT_DIR* of the *MY_XD_JOB_MOD_DS* record. The last step of the restore job 'step3-fileCleanUp'  then deletes these files.