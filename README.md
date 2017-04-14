# Deploy vert.x apps and mariadb

## 1. Vert.x apps
 - http://vertx.io/blog/using-the-asynchronous-sql-client/ 참고
 - Change
  * HSQL --> Mariadb 10.1.22
 - 변경된 소스 : 01.vertx-apps


## 2. Deploy mariadb container using docker
 - https://hub.docker.com/r/_/mariadb/ 참고
 ```
 # 1) mariadb container 배포 (3306 port open) -> SQuirrelSQL로 조회하는 용도
 # 만약 docker vertx에서만 조회한다면 port를 open할 필요가 없다. (나중에 docker link옵션 활용)
 # vert.x container에서 maridb container에 접속하기 위해서 some-mariadb 명칭으로 접근
 > docker run -p 3306:3306 --name some-mariadb -e MYSQL_ROOT_PASSWORD=my-secret-pw -d mariadb:10.1

 # 2) connect to mariadb container
 > docker exec -it some-mariadb bash

 # 3) connect to mariadb inside docker container
 root@fdd9c203e990:/# mysql -uroot -pmy-secret-pw  
 Welcome to the MariaDB monitor.  Commands end with ; or \g.
 Your MariaDB connection id is 97
 Server version: 10.1.22-MariaDB-1~jessie mariadb.org binary distribution

 Copyright (c) 2000, 2016, Oracle, MariaDB Corporation Ab and others.

 Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

 MariaDB [(none)]>
 ```

## 3. Deploy vertx apps container using docker
### 3.0) Prerequisit
 ```
 > cd ~
 > git clone https://github.com/freepsw/docker-vertx-mariadb.git
 > cd docker-vertx-mariadb/01.vertx-apps

 # build an create fat jar file
 > mvn clean package

 # download jar file for mariadb connection
 > mkdir lib
 > wget https://downloads.mariadb.com/Connectors/java/connector-java-1.5.5/mariadb-java-client-1.5.5.jar -P ./lib
 ```

### 3.1) Build vertx-apps docker image
 - http://vertx.io/docs/vertx-docker/ 참고
 - Dockerfile 내용 확인 (변경하고자 하는 설정이 있을 경우변경)
 ```
 # Extend vert.x image
 FROM vertx/vertx3

 # set the verticle class name and the jar file
 ENV VERTICLE_NAME io.vertx.blog.first.MyFirstVerticle
 ENV VERTICLE_FILE 01.vertx-apps/target/my-first-app-db-1.0-SNAPSHOT-fat.jar
 ENV VERTICLE_CONF_FILE 01.vertx-apps/src/main/conf/my-application-conf.json
 ENV VERTICLE_JDBC_FILE 01.vertx-apps/lib/mariadb-java-client-1.5.5.jar

 # Set the location of the verticles
 ENV VERTICLE_HOME /usr/verticles
 ENV CLASSPATH "/usr/verticles/mariadb-java-client-1.5.5.jar:/usr/verticles/my-first-app-db-1.0-SNAPSHOT-fat.jar

 EXPOSE 8080

 # Copy your verticle to the container
 COPY $VERTICLE_FILE $VERTICLE_HOME/
 COPY $VERTICLE_CONF_FILE $VERTICLE_HOME/
 COPY $VERTICLE_JDBC_FILE $VERTICLE_HOME/

 # Launch the verticle
 WORKDIR $VERTICLE_HOME
 ENTRYPOINT ["sh", "-c"]
 CMD ["exec vertx run $VERTICLE_NAME --conf $VERTICLE_HOME/my-application-conf.json"]
 ```
 - Builf docker image
 ```
 > docker build -t freepsw/vertx-java .
 # 정상적으로 docker image가 생성되었는지 확인
 > docker images
 ```

### 3-2) Run Docker image
 - Docker container를 실행하는 방법은 크게 2가지가 있다.
 - foregroud 방식
   * docker run으로 실행하면서, container로 바로 접속함.
   * 따라서 container에서 나오거나,
   * 해당 terminal이 종료되면, container도 중지됨.
   * 중지하지 않고 나오는 방법(CTRL + p)이 있으나, 불편함.
  - background 방식
   * container가 background로 동작하도록 한다. (-d 옵션 추가)
   * 나중에 해당 container에 접속하기 위해서는 docker exec명령로 접속

 ```
 # 1) foregroud 실행
 > docker run -ti -p 8082:8082 --link some-mariadb freepsw/vertx-java

 # 2) background 실행
 > docker run -d -ti -p 8082:8082 --link some-mariadb freepsw/vertx-java

 # 2-1) conntainer에 접속하기.
 # 실행중인 container id를 조회하고, 이를 이용해 접속
 > docker ps
 > docker exec -it container_id /bin/bash

 ```

## 4. Check using web browser
 - connnect to http://<ip>:8082/assets



## Vert.x configuration
### 1) Log Properties (logging.properties)
 - log configuration 위치
  * src/main/resources/vertx-default-jul-logging.properties
 - 주요 설정 파라미터
  * java.util.logging.FileHandler.pattern : log 파일이 생성될 위치 및 파일명
  * 아래와 같은 예약어를 사용 가능
  ```
  - “/” the local pathname separator
  - “%t” the system temporary directory
  - “%h” the value of the “user.home” system property
  - “%g” the generation number to distinguish rotated logs
  - “%u” a unique number to resolve conflicts
  - “%%” translates to a single percent sign “%”
  ```

### 2) config.json
 - mariadb 접속과 관련된 설정을 관리
 - vert.x container 실행시 --link로 some-mariadb를 연결했다면,
 - url의 ip를 아래와 같이 변경할 수 있다
   * "url": "jdbc:mariadb://some-mariadb:3306/mysql"

 ```
 {
   "http.port" : 8082,
   "url": "jdbc:mariadb://172.16.118.131:3306/mysql",
   "driver_class": "org.mariadb.jdbc.Driver",
   "user": "root",
   "password": "my-secret-pw",
   "maxPoolSize" : 20
 }
 ```
