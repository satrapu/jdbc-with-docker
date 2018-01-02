# jdbc-with-docker  

## Description  
This repo tackles the issue of controlling container startup order in Docker Compose.  
Additionally, the repo presents:
- A way of bundling a Java console application along with its dependencies using Maven [Assembly plugin](http://maven.apache.org/plugins/maven-assembly-plugin/index.html)
- A way of getting a JDBC driver using [Java SPI](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
- Debug a Java application running in a Docker container

This repo contains a docker-compose setup made out of 2 services: one database and one console application reading data from the former.   
The main issue of this setup is to ensure that the application service will wait until the database service is ready to process any incoming connections.  

## Solutions  
### Solution #1: Using healthcheck and depends_on Docker Compose directives  
#### Description
One possible approach for tackling this problem is to make use of the [healthcheck](https://docs.docker.com/compose/compose-file/compose-file-v2/#healthcheck) and [depends_on](https://docs.docker.com/compose/compose-file/compose-file-v2/#depends_on) Docker Compose directives:  
```yaml
version: '2.1'
services:
  db: ...
    healthcheck: ...
...
  app:
    ...
    depends_on:
      db:
        condition: service_healthy
...
```  

 :exclamation: This solution works only with Compose file versions 2.1, 2.2 and 2.3, since version 3 no longer supports the condition form of depends_on.  
See some reasoning for changing depends_on behaviour [here](https://github.com/docker/compose/issues/4305).

#### Setup  
* Clone this repo 
```bash
$ git clone git@github.com:satrapu/jdbc-with-docker.git
$ cd jdbc-with-docker
```  
* Inside the root folder of this repo, create an .env file with the following contents:
```properties
# The password of the MySQL "root" user account 
mysql_root_password=<PASSWORD_GOES_HERE>

# The name of the MySQL database to be created when first running the "db" Compose service
mysql_database_name=<NAME_GOES_HERE>

# The name of the user account used for accessing the newly created MySQL database
mysql_database_user=<USERNAME_GOES_HERE>

# The password of the user account used for accessing the newly created MySQL database
mysql_database_password=<PASSWORD_GOES_HERE>

# The space separated list of JVM flags to be used when running the Java console application
# inside the Docker container; see more here: https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html
java_jvm_flags=-Xmx512m

# The port used for debugging purposes
java_debug_port=9876

# The JVM flags used for enabling debugging.
# Pay special attention to the "suspend" argument: if set to "y', the JVM process will halt until a debugger its attached; 
# if set to "n", the debugger can be attached later, without initially halting the JVM process
java_debug_settings=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9876
``` 
* Open a terminal and run the following commands:  
```bash
# Run the default Maven build commands
mvn && \

# Stop the existing containers, if any, and remove any local content
docker-compose down --rmi local && \

# Build Docker image containing the Java 8 console application
docker-compose build && \

# Start the MySQL Docker container, wait for the database to be able to process incoming connections
# and only then start the Java console application Docker container
docker-compose up
```  

### Solution #2: Using a dependency checker service
#### Description
Another approach is to use a dependency checker service, __check_db_connectivity__, which will check whether the database is able to process incoming connections before starting the application:
```yaml
version: '2.1'
services:
  db:
    ...
  check_db_connectivity:
    image: activatedgeek/mysql-client:0.1
    entrypoint: ...
    depends_on:
      - db
  app:
   ...
   depends_on:
     - db
```

This new service is based on a [MySQL client Docker image](https://hub.docker.com/r/activatedgeek/mysql-client/) which will execute a SQL command from time to time via a simple [shell script](https://github.com/satrapu/jdbc-with-docker/blob/dariusz-pasciak-wait-for-dependencies/docker-compose.yml#L20).    
The __check_db_connectivity__ and __app__ services will be run using separate Docker Compose commands to ensure the latter will start only after the former has ended its database connectivity check.  
 
 This solution was inspired by [this blog entry](https://8thlight.com/blog/dariusz-pasciak/2016/10/17/docker-compose-wait-for-dependencies.html) and it's available inside branch [dariusz-pasciak-wait-for-dependencies](https://github.com/satrapu/jdbc-with-docker/blob/dariusz-pasciak-wait-for-dependencies).  
 :exclamation: This solution works with Compose file version 2 and up.  

#### Setup
* Clone this repo 
```bash
$ git clone git@github.com:satrapu/jdbc-with-docker.git
$ cd jdbc-with-docker
```  
* Inside the root folder of this repo, create an .env file with the following contents:
```properties
# The password of the MySQL "root" user account 
mysql_root_password=<PASSWORD_GOES_HERE>

# The name of the MySQL database to be created when first running the "db" Compose service
mysql_database_name=<NAME_GOES_HERE>

# The name of the user account used for accessing the newly created MySQL database
mysql_database_user=<USERNAME_GOES_HERE>

# The password of the user account used for accessing the newly created MySQL database
mysql_database_password=<PASSWORD_GOES_HERE>

# Try to connect to the DB each given amount of time (see more here: https://linux.die.net/man/1/sleep)
check_db_connectivity_interval=5s

# The total number of attempts before considering the DB is NOT open for incoming connections
check_db_connectivity_attempts=20

# The space separated list of JVM flags to be used when running the Java console application
# inside the Docker container; see more here: https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html
java_jvm_flags=-Xmx512m

# The port used for debugging purposes
java_debug_port=9876

# The JVM flags used for enabling debugging.
# Pay special attention to the "suspend" argument: if set to "y', the JVM process will halt until a debugger its attached; 
# if set to "n", the debugger can be attached later, without initially halting the JVM process
java_debug_settings=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9876
``` 

* Open a terminal and run the following commands:  
```bash
# Run the default Maven build commands
mvn && \

# Stop the existing containers, if any, and remove any local content
docker-compose down --rmi local && \

# Build Docker image containing the Java 8 console application
docker-compose build && \

# Start the MySQL Docker container, then the dependency checker container and then run the script which will try to connect to the database
# for a given amount of retries each several seconds.
docker-compose up check_db_connectivity && \

# Start the Java Docker container
docker-compose up app
```  

### Other Solutions
See [this page]( https://docs.docker.com/compose/startup-order) on Docker Compose documentation.

### Bonus
#### Create JAR using Maven Assembly plugin
TBD

#### Load JDBC driver using Java SPI
TBD

#### Debug the console application
* Ensure the **java_debug_settings** property found inside the .env file will halt the JVM until a debugger is attached via the "suspend=y" argument and will listen for a debugger on the port mentioned via "address=xxx" argument:
```properties
...
java_debug_settings=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9876
...
```  
* Find the host port mapped to the one exposed by the **app** service:  
```bash
docker-compose port --protocol=tcp app 9876
```  
* Configure your IDE debugger to connect to a remote Java application using the Docker host port of the app service
* Start compose
* Step into the source code of the application running in a Docker container
