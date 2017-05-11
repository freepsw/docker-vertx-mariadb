# Deploy vert.x apps and mariadb
- container 가상화(docker)를 이용하여 micro service를 빠르고 쉽게 배포하는 3가지 방법을 가이드
- 전체 서비스의 구성을 간략하게 설명하면...
 * mariadb 설치 및 서비스 구동 (필요한 database & table 생성)
 * vertx web application 설치 및 구동
 * load balancer 설치 및 vertx와 연결

##  위의 구성을 3가지 방식으로 배포
### 1안) docker command line을 이용하여 배포
- https://github.com/freepsw/docker-vertx-mariadb#1안-docker-command-line을-이용한-배포 참고
### 2안) docker-compose를 이용한 배포
- https://github.com/freepsw/docker-vertx-mariadb/tree/master/10.docker_compose 참고
### 3안) rancher를 이용한 배포
- https://github.com/freepsw/docker-vertx-mariadb/tree/master/11.rancher 참고





## 1안) Docker command line을 이용한 배포
### 1. Vert.x apps
 - http://vertx.io/blog/using-the-asynchronous-sql-client/ 참고
 - Change
  * HSQL --> Mariadb 10.1.22
 - 변경된 소스 : 01.vertx-apps


### 2. Deploy mariadb container using docker
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

### 3. Deploy vertx apps container using docker
#### 3.0) Prerequisit
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

#### 3.1) Build vertx-apps docker image
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
 # -cp : set classpath of jar file
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

#### 3-2) Run Docker image
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

#### 3-3).  Vert.x configuration

#####  Log Properties (logging.properties)
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

####  config.json
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

### 4. Check using web browser
 - connnect to http://<ip>:8082/assets




## Etc


### 1. Database import to Mariadb container when container initialized
- 많은 경우 application에 필요한 메타정보가 db에 저장되어 있어야 한다.
- 따라서 db container가 구동되는 시점에 해당 정보를 함께 db에 import하도록 함.

#### 1) Dump database to file
- Container에 import될 database를 dump하여 파일로 저장
 ```
 > mysqldump -uroot -pmy-secret-pw test_db > test_db.sql
 ```

#### 2) Copy dump file(test_db.sql) to localhost
- container에서 dump한 test_db.sql 파일을 lcoal directory로 복사한다.
 ```
 # container id 조회
 > docker ps
 CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                              NAMES
 a9ea3800efd7        mariadb:10.1        "docker-entrypoint..."   8 hours ago         Up 8 hours          0.0.0.0:3306->3306/tcp             some-mariadb

 # copy dump file to localhost
 # 위에서 조회한 container id(a9ea3800efd7)로 복사할 container를 지정
 > docker cp a9ea3800efd7:/root/test_db.sql /home/rts/apps/docker-vertx-mariadb/verticle_data
 ```

#### 3) Import dump file(test_db.sql) into maridb when maridb is initialized
 ```
 # 만약 database를 생성해야 한다면,
 # MYSQL_DATABASE 변수에 생성할 database명을 입력한다. (여기서는 test_db가 database명)
 > docker run -v <test_.sql 경로>:/docker-entrypoint-initdb.d -p 3306:3306 --name some-mariadb -e MYSQL_ROOT_PASSWORD=my-secret-pw -e MYSQL_DATABASE=test_db  -d mariadb:10.1
 ```



### 2. vert.x application 변경시 docker build 없이 docker run
- vert.x jar , config.json 파일이 docker image에 포함되어 있어서 변경시 docker image를 다시 빌드해야 함.
- 운영상황에서는 이 방식이 깔끔하고, 사용자의 실수가 없는 방식인데,
- 테스트 단계에서는 수많은 변경사항이 발생하게 된다. (결국 docker build가 너무 많이 발생, 시간 소요)
- 그래서 자주 변경되는 file(.jar, .json ...)을 local directory에서 읽어오도록 변경
- docker volum option 사용

#### 1) 새로운 Dockerfile 생성 (Dockerfile_var)
- Dockerfile 에서 .jar/.json를 읽어오는 부분을 제거
- $VERTICLE_HOME의 경로에 있어야 할 파일들을 확인힌다. (jar, json, lib)
```
 > cd docker-vertx-mariadb

 > vi Dockerfile_var

# Extend vert.x image
FROM vertx/vertx3

# set the verticle class name and the jar file
ENV VERTICLE_NAME io.vertx.blog.first.MyFirstVerticle

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles
ENV CLASSPATH "/usr/verticles/mariadb-java-client-1.5.5.jar:/usr/verticles/my-first-app-db-1.0-SNAPSHOT-fat.jar

EXPOSE 8080

# Launch the verticle
# -cp : set classpath of jar file
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME --conf $VERTICLE_HOME/my-application-conf.json"]
```

#### 2) docker run에서 -v 옵션 추가
- $VERTICLE_HOME에 필요한 파일을 특정 directory에 복사한다.
```
 > mkdir verticle_data
 > cp 01.vertx-apps/target/my-first-app-db-1.0-SNAPSHOT-fat.jar ./verticle_data
 > cp 01.vertx-apps/src/main/conf/my-application-conf.json ./verticle_data
 > cp 01.vertx-apps/lib/mariadb-java-client-1.5.5.jar ./verticle_data
```
- my-application-conf.json에서 jdbc설정을 변경한다.  (mysql -> test_db)
{
  "http.port" : 8082,
  "url": "jdbc:mariadb://some-mariadb:3306/test_db",
  "driver_class": "org.mariadb.jdbc.Driver",
  "user": "root",
  "password": "my-secret-pw",
  "maxPoolSize" : 20
}


- docker image를 build한다.

```
 >  docker build -t freepsw/vertx-java-var -f Dockerfile_var .
```

- docker run 실행
```
 > docker run -ti -p 8082:8082 -v ~/apps/docker-vertx-mariadb/verticle_data:/usr/verticles --link some-mariadb freepsw/vertx-java-var
```
