package hackathon

import play.{ Configuration, Play }
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Success, Failure }
import com.typesafe.config.ConfigFactory

object CDF {
  
  lazy val config = Try(Play.application().configuration()) match {
    case Success(c) => c
    case _ => new Configuration(ConfigFactory.load())
  }
  
  lazy val base = config.getString("cdf.url") + "/services-web/cdf/"
  
  // Get the information model URIs as (name, uri) pairs
  def infoModelUris()(implicit ec: ExecutionContext): Future[Seq[(String, String)]] = 
    WS.url(base + "metadata").get map { resp =>
      for {
        JsArray(infoModels) <- resp.json \\ "informationModels"
        JsObject(fields) <- infoModels
        (name, infoModel) <- fields
      } yield (name, (infoModel \ "url").as[String])
    }
  
  def allReducedEventsQuerySpec(start: Int, rows: Int)(implicit ec: ExecutionContext): Future[JsValue] = 
    WS.url(base + "/services-web/cdf/data/GDELT/Reduced_Events_all_QuerySpec?$start="+start+"&$rows="+rows).get map (_.json)
  
  def instances(urls: Seq[String])(implicit ec: ExecutionContext) = {
    val len = (base + "data/").length
    val resourceObjs = urls map { url => 
      JsObject(Seq("resource" -> JsString(url.substring(len)), "method" -> JsString("GET"))) 
    }
    val req = JsObject(Seq("keyed" -> JsArray(resourceObjs)))
    
    WS.url(base + "data/$keyed").withHeaders("Content-Type" -> "application/json").post(req) map { resp =>
      for {
        obj <- resp.json \\ "response"
      } yield (obj)
    }
  }
  
  def classDescription(classUrl: String)(implicit ec: ExecutionContext) = 
    WS.url(classUrl + "?$expand=true").get map { resp =>
      val attrTypes = for {
        JsArray(attrs) <- resp.json \\ "attributes"
        attr <- attrs
      } yield ((attr \ "identifier").as[String], (attr \ "type").as[String])
      
    }
}