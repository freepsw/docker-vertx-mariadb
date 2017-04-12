# Extend vert.x image
FROM vertx/vertx3

# set the verticle class name and the jar file
ENV VERTICLE_NAME io.vertx.blog.first.MyFirstVerticle
ENV VERTICLE_FILE 01.vertx-apps/target/my-first-app-db-1.0-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy your verticle to the container
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
