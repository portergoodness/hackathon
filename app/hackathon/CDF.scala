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
  
  lazy val hackathonUrl = config.getString("cdf.url")
  lazy val base = hackathonUrl + "/services-web/cdf/"
  lazy val solrBase = hackathonUrl + "/cdf-apache-solr/"
  
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
    WS.url(base + "data/GDELT/Reduced_Events_all_QuerySpec?$start="+start+"&$rows="+rows).get map (_.json)
  
  def reducedEventsQuerySpecByID(ids: List[Long])(implicit ec: ExecutionContext): Future[JsValue] =
    WS.url(base + "data/GDELT/Reduced_Events_all_QuerySpec?$constraint=" + ids.mkString("Id%20Eq%20", "%20or%20Id%20Eq%20", "")).get map (_.json)
  
  def allReducedEventsQuerySpecSolr(start: Int, rows: Int, search: Option[String])(implicit ec: ExecutionContext): Future[JsValue] = {
    val urlBase = hackathonUrl+"/cdf-apache-solr/select"
    val query = search match {
      case Some(searchText: String) => "SearchField%3A"+searchText
      case None 					=> "*%3A*"
    }
    val startUrl = "start="+start
    val rowsUrl = "rows="+rows
    val solrUrl = urlBase + "?" + "q=" + query + "&" + startUrl + "&" + rowsUrl + "&wt=json"
    WS.url(solrUrl).get map (_.json)
  }
  
  
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