package pl.deally {
package model {

import scala.xml.{NodeSeq}
import net.liftweb.http._
import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import S._
import pl.deally.lib._

object Location extends Location with LongKeyedMetaMapper[Location] 
with CRUDify[Long,Location] {
  
  override def dbTableName = "locations"
  
  override def pageWrapper(body: NodeSeq) =
    <lift:surround with="admin" at="content">{body}</lift:surround>
  override def calcPrefix = List("admin",_dbTableNameLC)
  override def showAllMenuLocParams = LocGroup("admin") :: Nil
  override def createMenuLocParams  = LocGroup("admin") :: Nil
  override def viewMenuLocParams    = LocGroup("admin") :: Nil
  override def editMenuLocParams    = LocGroup("admin") :: Nil
  override def deleteMenuLocParams  = LocGroup("admin") :: Nil
  
  override def _showAllTemplate = CrudifyHelpers.showAllTemplate
  override def _viewTemplate = CrudifyHelpers.viewTemplate
  override def _createTemplate = CrudifyHelpers.createTemplate
  override def _deleteTemplate = CrudifyHelpers.deleteTemplate
  override def _editTemplate = CrudifyHelpers.editTemplate
  
  val superUserLoggedIn = If(User.superUser_? _, CrudifyHelpers.loginAndComeBack _)
  override protected def addlMenuLocParams: List[Loc.AnyLocParam] = superUserLoggedIn :: Nil
  
}

class Location extends LongKeyedMapper[Location] with IdPK {

  def getSingleton = Location

  object city extends MappedString(this, 64) {
    override def displayName = ?("city")
  }
  object country extends MappedCountry(this) {
    override def displayName = ?("country")
  }
  object latitude extends MappedDouble(this) {
    override def displayName = ?("latitude")
  }
  object longitude extends MappedDouble(this) {
    override def displayName = ?("longitude")
  }

  def nearestCity(latitude: Double, longitude: Double): Box[String] = {
    val sql = "SELECT city, ((ACOS(SIN(%s*PI()/180)*SIN(latitude*PI()/180)"+
    "+COS(%s*PI()/180)*COS(latitude*PI()/180)*COS((%s-longitude)*PI()/180))"+
    "*180/PI())*111.19*1.1515) AS \"distance\" FROM locations ORDER BY distance ASC LIMIT 1"
    val query = sql.format(latitude, latitude, longitude)
    val result = Location.findAllByInsecureSql(query, IHaveValidatedThisSQL("czepol", "2011-08-10"))
    if(result.count(c =>true)==1)
      Full(result(0).city.toString)
    else
      None
  }

}

}
}
