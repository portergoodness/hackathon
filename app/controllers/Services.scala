package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import hackathon.{CDF, TestReducedEvents}
import play.api.libs.concurrent.Execution.Implicits._


object Services extends Controller {
  
  def getReducedEvents(start: Int, rows: Int) = Action {
    val promiseOfEvents = CDF.allReducedEventsQuerySpec(start, rows)
    Async {
      promiseOfEvents.map(f => {
        Ok(Json.toJson(f))
      })
      
    }
    
  }
  
  def getMetadata = Action {
    val promiseOfMetadata = CDF.infoModelUris()
    Async {
      promiseOfMetadata.map(f => {
        Ok(Json.toJson(f.toMap))
      })
      
    }
    
  }
  
  // returns test data
  def getTestReducedEvents = Action {
    Ok(TestReducedEvents.testData)
  }
  
  def receiveTrainingSetUrls = Action{ request =>
    
    request.body.asJson match {
      case jsonBody: Some[JsObject] => {
        Ok(Json.toJson(jsonBody))
      }
      case _ => BadRequest("Body not in Json")
    }
  }
  
  // Take a set of +/- 
  def classify = Action { req =>
    val posNegs = for {
      json <- req.body.asJson.toSeq
      JsArray(pos) <- json \\ "positives"
      JsArray(neg) <- json \\ "negatives"
    } yield (pos.map(_.as[String]), neg.map(_.as[String]))
    
    Ok("hey")
  }
  
}