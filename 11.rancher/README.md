# Run service using rancher
- docker-compose로 실행한 container를 rancher를 이용하여 service로 구성해보자.
- 먼저 Web UI를 이용하여 서비스를 구성해 보고,
- 다음으로 rancher cli로 rancher-compose.yml 과 docker-compose.yml을 이용하여 설치한다.

## 1. install rancher
- http://docs.rancher.com/rancher/v1.5/en/quick-start-guide/
- guide에 따라서 rancher server를 설치하고,
- container를 구동할 host 서버에 docker run 명령어를 실행한다.

## 2. Create service using racher web ui
- loadbalancer - vertx-web - mariadb로 연결되는 서비스를 생성한다.
- > STACKS > ADD Stack >
  > Add Service (vertx-web, some-mariadb)
    * docker-hub에 없는 image인 경우에는 "Always pull image before creating"을 uncheck해야 한다.
    * check하면 항상 docker-hub에서 image를 찾으려 하므로, image를 찾을 수 없다는 오류가 발생
  > Add loadbalancer (loadbalancer)
- docker-compose에 작성된 다양한 옵션값을 설정해야 함.
- 최종적으로 서비스가 구성되면 자동으로 docker-compose.yml과 rancher-compose.yml 파일이 생성된다.

### create service using docker-compose.yml and rancher-compose.yml
- 기존에 생성된 설정파일들이 있다면 이를 활용해서 바로 서비스를 생성할 수 있다.
> STACKS > Add Stack (여기서 docker-compose.yml, rancher-compose.yml 파일을 import)

## 3. Create service using rancher cli
- rancher web ui에서 API Key를 생성하여 access key/secret 코드를 복사한다.
- rancher cli 명령어를 다운받아서 실행가능한 경로로 복사한 후
- rancher server에 API Key를 등록한다.
- rancher up 명령어로 docker-compose.yml, rancher-compose.yml을 이용하여 stack 및 service를 생성한다.

```
 # 1) download rancher command
 > wget https://releases.rancher.com/cli/v0.5.0/rancher-linux-amd64-v0.5.0.tar.gz
 > tar xvf rancher-linux-amd64-v0.5.0.tar.gz
 > sudo cp rancher-v0.5.0/rancher /usr/local/bin/

 # 2) Add account API Key
 #    아래의 설정이 "/home/rts/.rancher/cli.json" 파일에 저장됨
 #    "{"accessKey":"82BEABA420F55EF34EFD","secretKey":"Gy3aG8byAgeAFhCU3GqMj6zjZn6g4HgT4nqr6LaG","url":"http://<rancher-server-ip>:8080/v2-beta/schemas","environment":"1a5"}"
 #    ansible로 배포하려면, 위의 cli.json을 해당 위치에 copy하면
 #    아래와 같은 rancher config를 실행하지 않아도 정상동작함.
 > rancher config
  URL []: http://<SERVER_IP>:8080/
  Access Key []: <accessKey_of_account_api_key>
  Secret Key []:  <secretKey_of_account_api_key>
 ""

 # 3) run rancher command
 > rancher up -d -s NewLetsChatApp
```
- Rancher cli에 대한 상세 옵션은 아래 링크 참조
- https://docs.rancher.com/rancher/v1.2/en/cli/

## 4. Connect vertx-web ui using web browser
http://172.16.118.132:8082/assets
