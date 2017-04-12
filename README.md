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
### 3.1) Build vertx docker image
