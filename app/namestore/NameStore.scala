package namestore

object NameStore {
  
  private var nameMap = Map[String, String]()
  
  def all = nameMap
  
  def byId(id: String) = nameMap get id
  
  def addName(name: String): String = {
    nameMap.find(entry => entry._2 == name) match {
      case Some(result) => result._1
      case None 		=> {
        val newKey = (nameMap.size + 1).toString
        nameMap += (newKey -> name)
        newKey
      }
    }
  }
  
  def deleteName(id: String) = {
    if (!nameMap.keySet.exists(_ == id)) {
      false
    } else {
      nameMap = nameMap - id
      true
    }
    
  }

}

