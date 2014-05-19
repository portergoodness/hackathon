package hackathon

import weka.classifiers.trees.SimpleCart2
import org.joda.time.format.ISODateTimeFormat

class CDFConstraintClassifier extends SimpleCart2 {
  import CDFType._
  
  private val dateTimeFmt = ISODateTimeFormat.dateTime()
  
  val worldBox = GeoBox(-180, 180, -90, 90)
  
  def getConstraints(): String = {
    val dnfConstraints = for {
      conjClause <- dnf(Nil, this) if (conjClause.last == Pos)
    } yield conjClause.dropRight(1)
    
    // This is kind of ugly
    val strConstraints: Seq[String] = dnfConstraints map { conjClause =>
      // Hack for geo constraints
      val geoConstPart = conjClause partition {
        case WekaConstraint(attr, _, _) => attr.split(":")(1) == "COORDINATE"
        case _ => false
      }
      
      // Fold geo constraints into a bounding box
      val geoBox = geoConstPart._1.foldLeft(("", worldBox)) {
        case ((aName, box), WekaConstraint(attr, op, Right(d))) => {
          val attrTypeArr = attr.split(":")
          val attrName = attrTypeArr(0)
          if (attrTypeArr.length == 3) (op, attrTypeArr(2)) match {
            case ("<", "Lat") => (attrName, box.copy(latMax = d))
            case (">=", "Lat") => (attrName, box.copy(latMin = d))
            case ("<", "Lon") => (attrName, box.copy(lonMax = d))
            case (">=", "Lon") => (attrName, box.copy(lonMin = d))
            case _ => (attrName, box)
          }
          else (attrName, box)
        }
        case (attrBox, _) => attrBox
      }
      
      val geoClause: Seq[String] = if (geoBox._2 != worldBox) {
        val attrName = geoBox._1
        val box = geoBox._2
        val points = Seq(box.lonMin + " " + box.latMin, box.lonMax + " " + box.latMin,
                         box.lonMin + " " + box.latMax, box.lonMax + " " + box.latMax)
        val polyStr = points.mkString("POLYGON(", ", ", ")")
        Seq("Within(" + attrName + ", " + polyStr + ")")
      }
      else Nil
      
      val strClause = geoConstPart._2 flatMap {
        case WekaConstraint(attr, op, value) => {
          val attrTypeArr = attr.split(":")
          val valStr = (cdfType(attrTypeArr(1)), value) match {
            case (CDFLong, Right(n)) => n.toLong.toString
            case (CDFString, Left(s)) => "'" + s + "'"
            case (CDFDouble, Right(n)) => n.toString
            case (CDFDate, Right(n)) => dateTimeFmt.print(n.toLong)
            case _ => ""
          }
          Seq(attrTypeArr(0) + " " + cdfOp(op) + " " + valStr)
        }
        case _ => Nil
      }
      (strClause ++ geoClause).mkString("(", " and ", ")")
    }
    
    strConstraints.mkString(" or ")
  }
  
  private def cdfOp(op: String) = op match {
    case "=" => "Eq"
    case "!=" => "Neq"
    case "<" => "Lt"
    case ">=" => "Ge"
  }
  
  private def dnf(constraints: Seq[Seq[WekaTerm]], cart: SimpleCart2): Seq[Seq[WekaTerm]] = {
    if (cart.getAttribute() == null) {
      val pn = if (cart.getClassValue() == 0.0) Neg else Pos
      constraints map { _ :+ pn }
    }
    else {
      val attrName = cart.getAttribute().name()
      val values = 
        if (cart.getAttribute().isNumeric()) {
          Seq(Right(cart.getSplitValue()))
        }
        else {
          // Split OR's into multiple values
          val vs = cart.getSplitString().split("\\|").toSeq
          vs map { v =>
            if (v.startsWith("(")) Left(v.slice(1, v.length - 1))
            else Left(v)
          }
        }
      
      val ops = if (cart.getAttribute().isNumeric()) ("<", ">=") else ("=", "!=")
      
      val leftCs = values map { v => Seq(WekaConstraint(attrName, ops._1, v)) }
      val rightCs = values map { v => Seq(WekaConstraint(attrName, ops._2, v)) }
      
      val left = dnf(leftCs, cart.getSuccessors()(0))
      val right = dnf(rightCs, cart.getSuccessors()(1))
      
      if (constraints.isEmpty) left ++ right
      else {
        val leftP = for {
          cs <- constraints
          ls <- left
        } yield cs ++ ls
        
        val rightP = for {
          cs <- constraints
          rs <- right
        } yield cs ++ rs
        
        leftP ++ rightP
      }
    }
  }
}

case class GeoBox(lonMin: Double, lonMax: Double, latMin: Double, latMax: Double)

sealed trait WekaTerm
case class WekaConstraint(attr: String, op: String, value: Either[String, Double]) extends WekaTerm
object Pos extends WekaTerm
object Neg extends WekaTerm