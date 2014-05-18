package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import hackathon.{ CDF, TestReducedEvents, Weka }
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import weka.classifiers.trees.SimpleCart


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
    
    // Get the +/- instances from CDF
    val pnUris = for {
      json <- req.body.asJson.toSeq
      JsArray(pos) <- json \\ "positives"
      JsArray(neg) <- json \\ "negatives"
      posInsts = CDF.instances(pos.map(_.as[String]))
      negInsts = CDF.instances(neg.map(_.as[String]))
    } yield (Await.result(posInsts, 20 seconds), Await.result(negInsts, 20 seconds))
    
    // Fold them into a single tuple
    val pnInsts = pnUris.foldLeft((Seq[JsValue](), Seq[JsValue]())) { 
      case((accPs, accNs), (ps, ns)) => (accPs ++ ps, accNs ++ ns)
    }
    
    // We assume all instances have the same CDF class/structure, so grab
    // the first pos or neg and get the class description as (attr, type) tuples
    val inst = pnInsts._1.headOption.getOrElse(pnInsts._2.head)
    val cls = (inst \ "__information" \ "classUrl").as[String]
    val clsDesc = CDF.classDescription(cls)
    
    val constraints = clsDesc map { attrs => 
      val wekaCont = Weka.instanceContainer(attrs)
      pnInsts._1 foreach { p => Weka.add(p, wekaCont, true) }
      pnInsts._2 foreach { n => Weka.add(n, wekaCont, false) }
      
      // Convert all string values to nominal for the CART classifier
      val classifier = new SimpleCart()
      classifier.buildClassifier(Weka.makeNominals(wekaCont.instances))
      classifier.toString()
    }
    
    Async {
      constraints map { Ok(_) }
    }
  }
  
}