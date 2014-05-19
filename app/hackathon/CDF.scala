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
  
  def reducedEventsQuerySpecByID(ids: List[Long])(implicit ec: ExecutionContext): Future[JsValue] = {
    
    val constraintsUrl = if (ids.length == 0) {
      ""
    } else if (ids.length == 1) {
      "?$constraint=Id%20Eq%20"+ids.head
    } else {
      "?$constraint=" + ids.mkString("Id%20In%20(", "%2C", ")")
    }
    val requestUrl = base + "data/GDELT/Reduced_Events_all_QuerySpec" + constraintsUrl
    println("CDF Query: "+requestUrl)
    WS.url(requestUrl).get map (_.json)
  }
  
  def allReducedEventsQuerySpecSolr(start: Int, rows: Int, search: Option[String], geoSearchParams: Option[(Float, Float, Float)])(implicit ec: ExecutionContext): Future[JsValue] = {
    val urlBase = hackathonUrl+"/cdf-apache-solr/select"
    val query = search match {
      case Some(searchText: String) => "SearchField%3A"+searchText.replaceAll(" ", "%20")
      case None 					=> "*%3A*"
    }
    val geoParamsUrl = geoSearchParams match {
      case Some(geoParams) => "fq=%7B!geofilt%20sfield=Location%7D&pt=" + geoParams._1 + "," + geoParams._2 + "&d=" + geoParams._3
      case None			   => ""
    }
    val startUrl = "start="+start
    val rowsUrl = "rows="+rows
    val solrUrl = urlBase + "?" + "q=" + query + "&" + startUrl + "&" + rowsUrl + "&" + geoParamsUrl + "&wt=json"
    println("Solr Query: "+solrUrl)
    WS.url(solrUrl).get map (_.json)
  }
  
  
  def instances(urls: Seq[String])(implicit ec: ExecutionContext): Future[Seq[JsValue]] = {
    val len = (base + "data/").length
    val resourceObjs = urls map { url => 
      JsObject(Seq("resource" -> JsString(url.substring(len)), "method" -> JsString("GET"))) 
    }
    val req = JsObject(Seq("keyed" -> JsArray(resourceObjs)))
    
    WS.url(base + "data/$keyed").withHeaders("Content-Type" -> "application/json").post(req) map { resp =>
      for {
        obj <- resp.json \ "keyed" \\ "response"
      } yield (obj)
    }
  }
  
  def classDescription(classUrl: String)(implicit ec: ExecutionContext): Future[Seq[(String, String)]] =
    WS.url(classUrl + "?$expand=true").get map { resp =>
      for {
        JsArray(attrs) <- resp.json \\ "attributes"
        attr <- attrs
      } yield ((attr \ "identifier").as[String], (attr \ "type").as[String])
    }
  
}