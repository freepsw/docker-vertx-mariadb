# Deploy vert.x apps and mariadb using docker_compose

# 0. Install docker_compose
- https://docs.docker.com/compose/install/
```
> sudo curl -L "https://github.com/docker/compose/releases/download/1.11.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

> sudo chmod +x /usr/local/bin/docker-compose
```




# 1. web application 확장을 위한 network 구성
- web 사용자가 많아지면서 web application을 확장할 경우,
- 동일한 ip:port로 접속하기 위해서는 L4 스위치와 같은 로드밸런서가 필요하다.
- 하지만 docker의 경우 새로운 container가 생성될 때 마다,
  외부에서 접속가능한 port가 랜덤으로 생성되므로 이를 효과적으로 지원하는 loadbalancer가 필요하다.
- 이번 예제에서 구성해볼 네트워크의 구성은

 1) browser(http://ip:8082/assets)

 2) lb_haproxy(8083 -> 80)
  * browser에서 8083로 접속한 port를 haproxy에 bind된 80 port로 연결
  * haproxy는 links에 등록된 "vertx-web"에 자동으로 연결한다. (8082)
  * 이때 haproxy에서 별도로 8082 port를 지정하지 않아도, links에서 인식하고 있는 vertx-web으로 연결된다.

 3) vertx-web(8082)
  * haproxy에서 요청한 api에 대한 결과를 browser에 전달

# 2. dockercloud/haproxy를 활용한 load balancing
- https://github.com/docker/dockercloud-haproxy 참고

## docker-compose 설정
- dockercloud/haproxy에서 vertx-web을 인식할 수 있도록 links에 추가
- 외부에서 haproxy에 접근할 수 있는 port와 haproxy의 80port 연결 (여기서는 8083:80)
- 전체 코드는 docker-compose.yml 참고
```
lb:
  image: dockercloud/haproxy
  depends_on:
    - 'vertx-web'
  links:
    - 'vertx-web'
  ports:
    - '8082:80'
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
```

# 3. docker-compose 실행 및 모니터링
## docker-compose 실행
```
# background로 실행시에는 -d 옵션 추가
> docker-compose up  

# 특정 docker-compose-test.yml을 사용할 경우
# 이후 모니터링 시에도 -f 옵션을 반드시 추가해야 함.
> docker-compose -f docker-compose-test.yml up
```

## docker-compose 모니터링
- 아래 결과를  보면 lb container의 port가 "0.0.0.0:8083->80/tcp"로 포워딩 되었다.
- http://<ip>:8083/assets으로 접속하면, vertx-web_1과 vertx-web_2에 자동으로 분산되어 접속한다.
```
> docker-compose ps
Name                             Command               State                    Ports
--------------------------------------------------------------------------------------------------------------------
dockervertxmariadb_lb_1             /sbin/tini -- dockercloud- ...   Up      1936/tcp, 443/tcp, 0.0.0.0:8083->80/tcp
dockervertxmariadb_some-mariadb_1   docker-entrypoint.sh mysql ...   Up      0.0.0.0:3306->3306/tcp
dockervertxmariadb_vertx-web_1      sh -c exec vertx run $VERT ...   Up      0.0.0.0:32817->8082/tcp
dockervertxmariadb_vertx-web_2      sh -c exec vertx run $VERT ...   Up      0.0.0.0:32816->8082/tcp
```

- 그런데 vertx-web은 0.0.0.0:32817->8082/tcp, 0.0.0.0:32816->8082/tcp 처럼 각자 port도 가지고 있다.
- 실제 web browser에서 http://<ip>:32817/assets로 접속해도 정상적으로 화면이 출력된다.
- 이유는 vertx-web의 설정에서 ports에 8082를 open하여 외부애서 직접 접속가능하도록 설정했기 때문이다.
- 만약 haproxy를 통해서만 접속하도록 하고 싶다면, ports를 삭제하면 된다.
```
vertx-web:
  image: 'freepsw/vertx-java-var'
  volumes:
    - ~/apps/docker-vertx-mariadb/verticle_data:/usr/verticles
  ports:
    - 8082
  links:
    - 'some-mariadb'
  depends_on:
    - 'some-mariadb'
```


- https://www.brianchristner.io/how-to-scale-a-docker-container-with-docker-compose/
