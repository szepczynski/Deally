package pl.deally {
package model {

import scala.xml.{NodeSeq}
import net.liftweb.http._
import net.liftweb.mapper._
import net.liftweb.common.{Full}
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import pl.deally.lib._
import S._

/**
 * Category model
 * @author Marcin Szepczyński
 * @since 0.1
 */

object Category extends Category with LongKeyedMetaMapper[Category] 
with CRUDify[Long,Category] {

  override def dbTableName = "categories"

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

class Category extends LongKeyedMapper[Category] 
with IdPK with OneToMany[Long, Category] {

  def getSingleton = Category

  object title extends MappedString(this, 128) {
    override def displayName = ?("title")
  }
  object slug extends MappedString(this, 64) {
    override def displayName = ?("slug")
    override def validations = 
      valUnique("Slug must be unique") _ :: super.validations
    override def setFilter = 
      HtmlHelpers.slugify _ :: super.setFilter
  }
  
  object deals extends MappedOneToMany(Deal, Deal.category, 
    OrderBy(Deal.start, Descending)) with Owned[Deal] with Cascade[Deal] 

  def withIdExist_?(categoryId: Long): Boolean = {
    Category.find(By(Category.id, categoryId)) match {
      case Full(category) => true
      case _ => false
    }
  }
  
  def idToString(id: Long): String = {
    Category.find(By(Category.id, id)) match {
      case Full(cat) => cat.title.is
      case _ => ""
    }
  }

}

}
}
