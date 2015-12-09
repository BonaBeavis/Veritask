package controllers

import java.util.concurrent.TimeUnit

import org.specs2.mutable._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent._
import scala.concurrent.duration._


/**
  * You can mock out a whole application including requests, plugins etc.
  * For more information, consult the wiki.
  */
class TasksetsIT extends Specification {

  val timeout: FiniteDuration = FiniteDuration(5, TimeUnit.SECONDS)

  val tasksetJson: JsValue = Json.parse(
    """
    {
        "uuid": "testuuid",
        "tasks": [
            {
                "s": "task1s",
                "p": "task1p",
                "o": "task1o"
            },
            {
                "s": "task2s",
                "p": "task2p",
                "o": "task2o"
            }
        ]
    }
    """)

  "Taskset API" should {

    "Create a Taskset from Json" in {
      running(FakeApplication()) {
        val request = FakeRequest.apply(POST, "/tasksets").withJsonBody(tasksetJson)
        val response = route(request)
        response.isDefined mustEqual true
        val result = Await.result(response.get, timeout)
        result.header.status must equalTo(CREATED)
      }
    }

    //    "fail inserting a non valid json" in {
    //      running(FakeApplication()) {
    //        val request = FakeRequest.apply(POST, "/user").withJsonBody(Json.obj(
    //          "firstName" -> 98,
    //          "lastName" -> "London",
    //          "age" -> 27))
    //        val response = route(request)
    //        response.isDefined mustEqual true
    //        val result = Await.result(response.get, timeout)
    //        contentAsString(response.get) mustEqual "invalid json"
    //        result.header.status mustEqual BAD_REQUEST
    //      }
    //    }
    //
    //    "update a valid json" in {
    //      running(FakeApplication()) {
    //        val request = FakeRequest.apply(PUT, "/user/Jack/London").withJsonBody(Json.obj(
    //          "firstName" -> "Jack",
    //          "lastName" -> "London",
    //          "age" -> 27,
    //          "active" -> true))
    //        val response = route(request)
    //        response.isDefined mustEqual true
    //        val result = Await.result(response.get, timeout)
    //        result.header.status must equalTo(CREATED)
    //      }
    //    }
    //
    //    "fail updating a non valid json" in {
    //      running(FakeApplication()) {
    //        val request = FakeRequest.apply(PUT, "/user/Jack/London").withJsonBody(Json.obj(
    //          "firstName" -> "Jack",
    //          "lastName" -> "London",
    //          "age" -> 27))
    //        val response = route(request)
    //        response.isDefined mustEqual true
    //        val result = Await.result(response.get, timeout)
    //        contentAsString(response.get) mustEqual "invalid json"
    //        result.header.status mustEqual BAD_REQUEST
    //      }
    //    }

  }
}