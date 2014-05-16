package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import hackathon.{CDF, TestReducedEvents}
import play.api.libs.concurrent.Execution.Implicits._


object Services extends Controller {
  
  // lookup stuff from solr
  def searchForEvents(start: Int, rows: Int, search: Option[String]) = Action {
    val promiseOfEvents = CDF.allReducedEventsQuerySpecSolr(start, rows, search)
    Async {
      promiseOfEvents.map(f => {
        Ok(Json.toJson(f))
      })
      
    }
    
  }
  
  // lookup CDF event data for the provided ids
  def lookupCDFEventsFromIds(ids: List[Long]) = Action {
    val promiseOfEvents = CDF.reducedEventsQuerySpecByID(ids)
    Async {
      promiseOfEvents.map(f => {
        Ok(Json.toJson(f))
      })
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