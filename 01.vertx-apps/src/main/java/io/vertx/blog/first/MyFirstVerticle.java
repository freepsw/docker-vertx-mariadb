package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This is a verticle. A verticle is a _Vert.x component_. This verticle is implemented in Java, but you can
 * implement them in JavaScript, Groovy or even Ruby.
 */
public class MyFirstVerticle extends AbstractVerticle {

  private static Logger logger = LoggerFactory.getLogger(MyFirstVerticle.class);

  private JDBCClient jdbc;

  /**
   * This method is called when the verticle is deployed. It creates a HTTP server and registers a simple request
   * handler.
   * <p/>
   * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP server has been
   * bound on the port, it call the `complete` method to inform that the starting has completed. Else it reports the
   * error.
   *
   * @param fut the future
   */
  @Override
  public void start(Future<Void> fut) {

    // Create a JDBC client
    jdbc = JDBCClient.createShared(vertx, config(), "My-Whisky-Collection");

    startBackend(
        (connection) -> createSomeData(connection,
            (nothing) -> startWebApp(
                (http) -> completeStartup(http, fut)
            ), fut
        ), fut);
  }

  private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
    jdbc.getConnection(ar -> {
      if (ar.failed()) {
        fut.fail(ar.cause());
      } else {
        next.handle(Future.succeededFuture(ar.result()));
      }
    });
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
    // Create a router object.
    Router router = Router.router(vertx);

    // Bind "/" to our hello message.
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application</h1>");
    });

    router.route("/assets/*").handler(StaticHandler.create("assets"));

    router.get("/api/whiskies").handler(this::getAll);
    router.route("/api/whiskies*").handler(BodyHandler.create());
    router.post("/api/whiskies").handler(this::addOne);
    router.get("/api/whiskies/:id").handler(this::getOne);
    router.put("/api/whiskies/:id").handler(this::updateOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);
    router.get("/api/test").handler(this::testFun);


    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
            config().getInteger("http.port", 8080),
            next::handle
        );
  }

  private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
    if (http.succeeded()) {
      fut.complete();
    } else {
      fut.fail(http.cause());
    }
  }


  @Override
  public void stop() throws Exception {
    // Close the JDBC client.
    jdbc.close();
  }

  private void addOne(RoutingContext routingContext) {
    jdbc.getConnection(ar -> {
      System.out.println("CONNECTION INSERT OK");
      // Read the request's content and create an instance of Whisky.
      final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(),
          Whisky.class);
      SQLConnection connection = ar.result();
      insert(whisky, connection, (r) ->
          routingContext.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(r.result())));
          connection.close();
    });

  }

  private void getOne(RoutingContext routingContext) {
    System.out.println("[TEST ] ID = " + routingContext.request().getParam("id"));
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      jdbc.getConnection(ar -> {
        // Read the request's content and create an instance of Whisky.
        System.out.println("CONNECTION SELECT OK");
        SQLConnection connection = ar.result();
        select(id, connection, result -> {
          if (result.succeeded()) {
            routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(result.result()));
          } else {
            routingContext.response()
                .setStatusCode(404).end();
          }
          connection.close();
        });
      });
    }
  }

  private void updateOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.getBodyAsJson();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      jdbc.getConnection(ar ->
          update(id, json, ar.result(), (whisky) -> {
            if (whisky.failed()) {
              routingContext.response().setStatusCode(404).end();
            } else {
              routingContext.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(Json.encodePrettily(whisky.result()));
            }
            ar.result().close();
          })
      );
    }
  }

  private void deleteOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      jdbc.getConnection(ar -> {
        SQLConnection connection = ar.result();
        connection.execute("DELETE FROM Whisky WHERE id='" + id + "'",
            result -> {
              routingContext.response().setStatusCode(204).end();
              connection.close();
            });
      });
    }
  }

  private void getAll(RoutingContext routingContext) {
    jdbc.getConnection(ar -> {
      SQLConnection connection = ar.result();
      connection.query("SELECT * FROM Whisky", result -> {
        System.out.println("[GetALL] NUMBER = " + result.result().getNumRows());
        logger.info("LOGGER INFO TEST!!");
        logger.error("LOGGER ERROR TEST!!");
        System.out.println("[GetALL] CONTENTS = " + result.result().getResults().get(0).encodePrettily());

        List<Whisky> whiskies = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
        routingContext.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(whiskies));
        connection.close();
      });
    });
  }

  private void createSomeData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
    System.out.println("CreateSomeData CALL!!");
    if (result.failed()) {
      fut.fail(result.cause());
    } else {
      SQLConnection connection = result.result();
      connection.execute(
          "CREATE TABLE IF NOT EXISTS Whisky (id MEDIUMINT NOT NULL AUTO_INCREMENT, name varchar(100), origin varchar(100), PRIMARY KEY (id))",
          ar -> {
            if (ar.failed()) {
              fut.fail(ar.cause());
              connection.close();
              return;
            }
            connection.query("SELECT * FROM Whisky", select -> {
              if (select.failed()) {
                fut.fail(ar.cause());
                connection.close();
                return;
              }
              System.out.println("Return Num = " + select.result().getNumRows());
              if (select.result().getNumRows() == 0) {
                insert(
                    new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"), connection,
                    (v) -> insert(new Whisky("Talisker 57° North", "Scotland, Island"), connection,
                        (r) -> {
                          next.handle(Future.<Void>succeededFuture());
                          connection.close();
                        }));
              } else {
                next.handle(Future.<Void>succeededFuture());
                connection.close();
              }
            });

          });
    }
  }

  private void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
    String sql = "INSERT INTO Whisky (name, origin) VALUES (?, ?)";
    connection.updateWithParams(sql,
        new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
        (ar) -> {
          if (ar.failed()) {
            next.handle(Future.failedFuture(ar.cause()));
            connection.close();
            return;
          }
          UpdateResult result = ar.result();
          // Build a new whisky instance with the generated id.
          // 왜 Whisky 객체를 만들까? 사용되는 곳도 없는것 같은데...
          // 일단 위의 insert에서는 없는 id(result.getKeys())를 입력해 준다. 어디에 쓰려고?
          Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
          next.handle(Future.succeededFuture(w));
        });
  }

  private void select(String id, SQLConnection connection, Handler<AsyncResult<Whisky>> resultHandler) {
    System.out.println("[TEST]  SELECT REQURST !!!");
    connection.queryWithParams("SELECT * FROM Whisky WHERE id=1", new JsonArray().add(id), ar -> {
      if (ar.failed()) {
        resultHandler.handle(Future.failedFuture("Whisky not found"));
      } else {
        if (ar.result().getNumRows() >= 1) {
          System.out.println(ar.result().getResults().get(0).encodePrettily());
          System.out.println(ar.result().getRows().get(0).fieldNames());
          System.out.println(ar.result().getRows().get(0).encodePrettily());

          resultHandler.handle(Future.succeededFuture(new Whisky(ar.result().getRows().get(0))));
        } else {
          System.out.println("Whisky is not found!!");
          resultHandler.handle(Future.failedFuture("Whisky not found"));
        }
      }
    });
  }

  private void update(String id, JsonObject content, SQLConnection connection,
                      Handler<AsyncResult<Whisky>> resultHandler) {
    String sql = "UPDATE Whisky SET name=?, origin=? WHERE id=?";
    connection.updateWithParams(sql,
        new JsonArray().add(content.getString("name")).add(content.getString("origin")).add(id),
        update -> {
          if (update.failed()) {
            resultHandler.handle(Future.failedFuture("Cannot update the whisky"));
            return;
          }
          if (update.result().getUpdated() == 0) {
            resultHandler.handle(Future.failedFuture("Whisky not found"));
            return;
          }
          resultHandler.handle(
              Future.succeededFuture(new Whisky(Integer.valueOf(id),
                  content.getString("name"), content.getString("origin"))));
        });
  }

  private void testFun(RoutingContext routingContext) {
    System.out.println("[TEST ] Test Function ");
    routingContext.response().setStatusCode(400).end();
/*      jdbc.getConnection(res -> {
      if (res.succeeded()) {

        SQLConnection connection = res.result();

        connection.query("SELECT * FROM Whisky where id=52", res2 -> {
          if (res2.succeeded()) {

            ResultSet rs = res2.result();
            for (JsonArray line : res2.result().getResults()) {
              System.out.println(line.encode());
            }
          }
        });
      } else {
        // Failed to get connection - deal with it
      }
          });*/
//
//        // Read the request's content and create an instance of Whisky.
//        System.out.println("CONNECTION SELECT OK");
//        SQLConnection connection = ar.result();
//        select(id, connection, result -> {
//          if (result.succeeded()) {
//            routingContext.response()
//                    .setStatusCode(200)
//                    .putHeader("content-type", "application/json; charset=utf-8")
//                    .end(Json.encodePrettily(result.result()));
//          } else {
//            routingContext.response()
//                    .setStatusCode(404).end();
//          }
//          connection.close();
//        });

  }
}
