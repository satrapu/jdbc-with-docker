# jdbc-with-docker  

## Description  
This repo contains a docker-compose setup made out of 2 services: one database and one console application reading data from the former.  
The main issue of this setup is to ensure that the application service will wait until the database service is ready to process any incoming connections.  

## Solutions  
### Solution #1: Docker-Compose healthcheck and depends_on directives  
One possible approach for tackling this problem is to make use of the [healthcheck](https://docs.docker.com/compose/compose-file/compose-file-v2/#healthcheck) and [depends_on](https://docs.docker.com/compose/compose-file/compose-file-v2/#depends_on) directives from docker-compose.yml file:
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

__This solution works only with Compose file versions 2.1, 2.2 and 2.3.__  
__Version 3 no longer supports the condition form of depends_on.__  

### Solution #2: TBD  

### Setup  
* Clone this repo  
* Open a terminal and run the following commands:  
```bash
# Run the default Maven build commands
mvn && \

# Stop the existing containers, if any, and remove any local content
docker-compose down --rmi local && \

# Build Docker image containing the Java 8 console application
docker-compose build && \

# Start the MySQL Docker container, wait for the database to be able to process incoming connections and onlu then start the Java Docker container
docker-compose up
```  
