package io.vertx.blog.first;

import com.jayway.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * These tests checks our REST API.
 */
public class MyRestIT {

  @BeforeClass
  public static void configureRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = Integer.getInteger("http.port", 8082);
  }

  @AfterClass
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  @Test
  public void checkThatWeCanRetrieveIndividualProduct() {
    // Get the list of bottles, ensure it's a success and extract the first id.
    final int id = 52;
    System.out.println("ID = " + id);

    get("/api/test/").then()
        .assertThat()
        .statusCode(400);
//    final int id = get("/api/whiskies").then()
//        .assertThat()
//        .statusCode(200)
//        .extract()
//        .jsonPath().getInt("find { it.name=='Bowmore 15 Years Laimrig' }.id");
//
//    System.out.println("Test00 OK");

    // Now get the individual resource and check the content
//    get("/api/whiskies/" + id).then()
//        .assertThat()
//        .statusCode(200)
//        .body("name", equalTo("Bowmore 15 Years Laimrig"))
//        .body("origin", equalTo("Scotland, Islay"))
//        .body("id", equalTo(id));
//    System.out.println("Test01 OK");
  }

//  @Test
//  public void checkWeCanAddAndDeleteAProduct() {
//    // Create a new bottle and retrieve the result (as a Whisky instance).
//    Whisky whisky = given()
//        .body("{\"name\":\"Jameson2\", \"origin\":\"Ireland3\"}").request().post("/api/whiskies").thenReturn().as(Whisky.class);
//    assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
//    assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
//    assertThat(whisky.getId()).isNotZero();
//    System.out.println("Test1 OK");
//    System.out.println("Test1 id = " + whisky.getId());
//
//
//    // Check that it has created an individual resource, and check the content.
//    get("/api/whiskies/" + whisky.getId()).then()
//        .assertThat()
//        .statusCode(200)
//        .body("name", equalTo("Jameson"))
//        .body("origin", equalTo("Ireland"))
//        .body("id", equalTo(whisky.getId()));
//    System.out.println("Test2 OK");
//
//    // Delete the bottle
//    delete("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(204);
//    System.out.println("Test3 OK");
//
//    // Check that the resource is not available anymore
//    get("/api/whiskies/" + whisky.getId()).then()
//        .assertThat()
//        .statusCode(404);
//
//    System.out.println("Test5 OK");
//  }
}
