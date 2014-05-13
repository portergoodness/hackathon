package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import hackathon.{CDF, TestReducedEvents}
import play.api.libs.concurrent.Execution.Implicits._


object Services extends Controller {
  
  def getReducedEvents = Action {
    val promiseOfStuff = CDF.infoModelUris()
    Async {
      promiseOfStuff.map(f => {
        Ok(Json.toJson(f.toMap))
      })
      
    }
    
  }
  
  // returns test data
  def getTestReducedEvents = Action {
    Ok(TestReducedEvents.testData)
  }
  
}