package hackathon

import weka.core.{ Attribute, FastVector, Instances, Instance, Range }
import weka.filters.unsupervised.attribute.StringToNominal
import weka.filters.Filter
import play.api.libs.json._
import org.joda.time.format.ISODateTimeFormat

object Weka {
  
  import CDFType._
  import WekaType._
  
  private val boolNominal = {
    val bools = new FastVector()
    bools.addElement("false")
    bools.addElement("true")
    bools
  }
  
  private val nullVec: FastVector = null
  
  private val dateTimeFmt = ISODateTimeFormat.dateTime()
  
  /** Get the weka format (instance container) based on a set of CDF attribute name/type pairs */
  def instanceContainer(attrNameTypePairs: Seq[(String, String)]): CDFInstances = {
    val pairs = attrNameTypePairs map { p => (p._1, cdfType(p._2)) }
    
    // Filter out the "Id", we don't want to learn it
    val filtPairs = pairs filter { _._1 != "Id" }
    
    // Fold the attribute name / type pairs into a weka vector
    val attrs = filtPairs.foldLeft(new FastVector()) { (vec, p) =>
      wekaAttrs(p) foreach { vec.addElement(_) }
      vec
    }
    
    // Add the class label
    val cls = new Attribute("Class", boolNominal)
    attrs.addElement(cls)
    
    val instances = new Instances("cdf", attrs, 0)
    instances.setClass(cls)
    
    CDFInstances(filtPairs.toMap, instances)
  }
  
  /** Add a CDF data instance to the container */
  def add(data: JsValue, inst: CDFInstances, pos: Boolean): CDFInstances = {
    data match {
      case JsObject(attrValues) => {
        val instData = Array.fill(inst.instances.numAttributes())(0.0)
        
        // Filter out the id attr
        val filtAttrs = attrValues filter { _._1 != "Id" }
        
        // Map/add the attribute values
        for {
          attrVal <- filtAttrs 
          (idx, wekaVal) <- wekaValues(attrVal, inst)
        } instData(idx) = wekaVal
        
        // Set the class (+/-)
        instData(instData.length - 1) = boolNominal.indexOf(pos.toString)
        
        inst.instances.add(new Instance(1.0, instData))
        
        inst
      }
      case _ => inst
    }
  }
  
  /** Turn any string attributes into nominal values */
  def makeNominals(instances: Instances): Instances = {
    // Find the indices that are string attrs
    val indices = Array.tabulate(instances.numAttributes())(i => i) filter { idx =>
      instances.attribute(idx).isString()  
    }
    if (indices.isEmpty) {
      instances
    }
    else {
      val filt = new StringToNominal()
      filt.setAttributeRange(Range.indicesToRangeList(indices))
      filt.setInputFormat(instances)
      Filter.useFilter(instances, filt)
    }
  }
  
  private def wekaValues(attrValue: (String, JsValue), inst: CDFInstances): Seq[(Int, Double)] = {
    val cdfType = inst.cdfAttrTypes.get(attrValue._1).getOrElse(CDFUnknown)
    val wekaTypes = wekaAttrTypes((attrValue._1, cdfType))
    
    (attrValue._2, wekaTypes) match {
      case (value, Seq((name, wType))) => {
        wekaAttrValue(name, value, wType, inst)
      }
      case (JsArray(values), nameTypePairs) => {
        values.zip(nameTypePairs) flatMap {
          case (value, (name, wType)) => wekaAttrValue(name, value, wType, inst)
        }
      }
      // TODO: How to handle enriched vals
      case _ => Nil
    }
  }
  
  /** Get the weka index/value pair for a CDF/JavaScript name/value pair */
  private def wekaAttrValue(name: String, 
                            value: JsValue, 
                            wType: WekaType, 
                            inst: CDFInstances): Seq[(Int, Double)] = {
    val opt: Option[(Int, Double)] = Option(inst.instances.attribute(name)) map { attr =>
      (value, wType) match {
        case (JsNumber(n), WekaNumeric) => (attr.index(), n.toDouble)
        case (JsString(s), WekaString) => (attr.index(), attr.addStringValue(s).toDouble)
        case (JsString(s), WekaDate) => (attr.index(), dateTimeFmt.parseDateTime(s).getMillis())
        case (JsBoolean(b), WekaNominal) => (attr.index(), boolNominal.indexOf(b.toString))
        case _ => null // no conversion
      }  
    }
    
    opt.toSeq
  }
  
  /** Map a CDF attribute name/type pair to weka Attribute(s) */
  private def wekaAttrs(cdfAttr: (String, CDFType)): Seq[Attribute] = 
    wekaAttrTypes(cdfAttr) map {
      case (name, WekaNumeric) => new Attribute(name)
      case (name, WekaString) =>  new Attribute(name, nullVec)
      case (name, WekaDate) =>    new Attribute(name)
      case (name, WekaNominal) if (cdfAttr._2 == CDFBoolean) => new Attribute(name, boolNominal)
      case _ => ???
    }
  
  /** Get the mappings from a given CDF name/type pair to equivalent Weka types.
   *  This is where any special cases are currently handled, e.g. coordinates
   *  split into separate numeric lat/lon fields, etc. */
  private def wekaAttrTypes(cdfAttr: (String, CDFType)): Seq[(String, WekaType)] = cdfAttr match {
    // In an ideal world this would be configurable, but this is a hack...so we have special
    // cases for "enrichment" hard-coded here, e.g.:
//  case ("CAMEOCode", CDFString) => ???
    // The following are the "general" mappings, i.e., not "enriched" in any way
    case (name, CDFLong) =>       Seq((name + ":LONG", WekaNumeric))
    case (name, CDFDouble) =>     Seq((name + ":DOUBLE", WekaNumeric))
    case (name, CDFString) =>     Seq((name + ":STRING", WekaString))
    case (name, CDFBoolean) =>    Seq((name + ":BOOLEAN", WekaNominal))
    case (name, CDFCoordinate) => Seq((name + ":COORDINATE:Lat", WekaNumeric), 
                                      (name + ":COORDINATE:Lon", WekaNumeric))
    case (name, CDFDate) =>       Seq((name + ":DATE", WekaDate))
    case (name, CDFDateRange) =>  Seq((name + ":DATERANGE:Start", WekaDate),
                                      (name + ":DATERANGE:End", WekaDate))
    case (name, CDFDoubleUnit) => Seq((name + ":DOUBLEUNIT:Value", WekaNumeric), 
                                      (name + ":DOUBLEUNIT:Units", WekaString))
    case _ =>                     Nil // don't currently handle
  }
  
}

case class CDFInstances(cdfAttrTypes: Map[String, CDFType.Value], instances: Instances)

object CDFType extends Enumeration {
  type CDFType = Value
  
  val CDFBoolean, CDFString, CDFLong, CDFDouble, CDFDate, CDFCoordinate,
      CDFDateRange, CDFGeometry, CDFReference, CDFDoubleUnit, CDFUnknown = Value
      
  def cdfType(s: String) = s match {
    case "BOOLEAN" => CDFBoolean
    case "STRING" => CDFString
    case "LONG" => CDFLong
    case "DOUBLE" => CDFDouble
    case "DATE" => CDFDate
    case "COORDINATE" => CDFCoordinate
    case "DATERANGE" => CDFDateRange
    case "GEOMETRY" => CDFGeometry
    case "REFERENCE" => CDFReference
    case "DOUBLEUNIT" => CDFDoubleUnit
    case _ => CDFUnknown
  }
}

object WekaType extends Enumeration {
  type WekaType = Value
  
  val WekaNumeric, WekaNominal, WekaDate, WekaString = Value
}