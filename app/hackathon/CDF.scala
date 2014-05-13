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
  
  lazy val base = config.getString("cdf.url")
  
  // Get the information model URIs as (name, uri) pairs
  def infoModelUris()(implicit ec: ExecutionContext): Future[Seq[(String, String)]] = 
    WS.url(base + "/services-web/cdf/metadata").get map { resp =>
      for {
        JsArray(infoModels) <- resp.json \\ "informationModels"
        JsObject(fields) <- infoModels
        (name, infoModel) <- fields
      } yield (name, (infoModel \ "uri").as[String])
    }
  
  def allReducedEventsQuerySpec(page: Int, pageSize: Int)(implicit ec: ExecutionContext): Future[Seq[(String, String)]] = 
    WS.url(base + "/services-web/cdf/data/GDELT/Reduced_Events_all_QuerySpec").get map { resp =>
      for {
        JsArray(infoModels) <- resp.json \\ "informationModels"
        JsObject(fields) <- infoModels
        (name, infoModel) <- fields
      } yield (name, (infoModel \ "uri").as[String])
    }
}