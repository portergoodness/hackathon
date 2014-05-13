package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._


import namestore._


object Services extends Controller {
  
  def getNames = Action {
    
    Ok(Json.toJson(NameStore.all))
  }
  
  def getName(id: String) = Action {
    NameStore.byId(id) match {
      case Some(name) => Ok(name)
      case None		  => NotFound("ID not found")	
    }
  }
  
  def setName(name: String) = Action { 
    
	  Ok(NameStore.addName(name))
  }
  
  def deleteName(id: String) = Action {
	  if (NameStore.deleteName(id)) {
	    Ok("")
	  } else {
		NotFound("Item with id "+id+" was not found")
	  }
  }
  
}