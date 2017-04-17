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

EXPOSE 8082

# Copy your verticle to the container
COPY $VERTICLE_FILE $VERTICLE_HOME/
COPY $VERTICLE_CONF_FILE $VERTICLE_HOME/
COPY $VERTICLE_JDBC_FILE $VERTICLE_HOME/

# Launch the verticle
# -cp : set classpath of jar file
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME --conf $VERTICLE_HOME/my-application-conf.json"]
