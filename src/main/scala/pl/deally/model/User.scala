package pl.deally {
package model {

import scala.xml.{NodeSeq}
import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.http._  
import js._  
import JsCmds._  

import Helpers._
import S.?

/**
 * User model inheriting MegaProtoUser.
 * @author Marcin Szepczynski
 * @since 0.1
 */

object User extends User with MetaMegaProtoUser[User] {

  type UserFields = MappedField[_, User]
    
  @volatile private var currentUserIds: Set[Long] = Set() 

  def isLoggedIn(who: User) = currentUserIds.contains(who.id.is)
   
  def loggedInUsers(): List[Long] = currentUserIds.toList 
  
  override def logUserIn(who: User) {
    super.logUserIn(who) 
    this.synchronized(currentUserIds += who.id.is)
  } 
  
  override def logUserOut() {
    currentUser match { 
      case Full(user) => {
        val id = user.id.toLong
        this.synchronized(currentUserIds -= id)
        super.logUserOut()
      }
      case _ => Nil
    }
  }


  // Name of db table
  override def dbTableName = "users"
   
  override val basePath: List[String] = "user" :: Nil
  override def signUpSuffix = "register"
  override def loginSuffix  = "login"
  override def lostPasswordSuffix = "lost-password"
  override def passwordResetSuffix = "reset-password"
  override def changePasswordSuffix = "change-password"
  override def logoutSuffix = "logout"
  override def editSuffix = "edit"
  override def validateUserSuffix = "validate-user"
  //override def homePage = if (loggedIn_?) "/dashboard" else "/"  

 
  override def skipEmailValidation = false
  
  override def fieldOrder = 
    List(id, userName, firstName, lastName, email, locale, timezone, password)

  def moderator_? : Boolean = currentUser.map(_.moderator.is) openOr false

  override def signupFields: List[UserFields] = 
    List(userName, email, password)

  override def screenWrap = 
    Full(<lift:surround with="default" at="content"><lift:bind /></lift:surround>)
  
  override def loginXhtml = {  
    (<form method="post" action={S.uri}>
     <fieldset>
      <legend>{S.??("log.in")}</legend>  
      <div class="clearfix">
        <label>{S.?("username")}</label>
        <div class="input"><user:username /></div>
      </div>  
      <div class="clearfix">
        <label>{S.??("password")}</label>
        <div class="input">
          <user:password />
        </div>
      </div>  
      <user:next />
      <a href={lostPasswordPath.mkString("/", "/", "")}>{S.??("recover.password")}</a>
      <div class="actions"><user:submit /></div>
     </fieldset>
     </form>)  
  }  
  
  override def login = { 
    if (S.post_?) {  
      S.param("username").  
      flatMap(username => getSingleton.find(By(userName, username))) match {  
        case Full(user) if user.validated &&  
          user.password.match_?(S.param("password").openOr("*")) =>  
          S.notice(S.??("logged.in"))  
          logUserIn(user)
          currentUser.open_!.lastLogin(new java.util.Date).save
          val redir = loginReferer.is match {  
            case url: String if url != "/" =>
              loginReferer("/")  
              url  
            case _ => {
              S.param("next").map(_.toString) match {
                case Full(next) => next
                case _ => homePage
              }
            }  
          }  
          S.redirectTo(redir)  
        
        // User account is unconfirmed
        case Full(user) if !user.validated =>  
          S.error(S.??("account.validation.error"))  
        
        // Invalid username/passowrd
        case _ => S.error(S.??("invalid.credentials"))  
      }  
    }
    val next = S.param("next").map(_.toString) match {
      case Full(next) => next
      case _ => homePage
    }
    bind("user", loginXhtml,  
         "username" -> (FocusOnLoad(<input type="text" name="username" class="xlarge" />)),  
         "password" -> (<input type="password" name="password" class="xlarge" />),
         "next" -> (<input type="hidden" name="next" value={next}/>),
         "submit" -> (<input type="submit" value={S.??("log.in")} class="btn primary" />))  
  }
   
  override def signupXhtml(user: User) = {  
    (<form method="post" action={S.uri}>
     <fieldset>
       <legend>{ S.??("sign.up") }</legend>  
          {localForm(user, false)}  
          <div class="actions"><user:submit class="btn primary" /></div>
          <div class="social-signup"><ul>
            <li><a href="/auth/facebook/signin"><img src="/img/signin_fb.png" alt="" /></a></li>
            <li><a href="/auth/twitter/signin"><img src="/img/signin_tw.png" alt="" /></a></li>
          </ul></div>
     </fieldset>
     </form>)  
  }  
  
  protected def localForm(user: User, ignorePassword: Boolean): NodeSeq = {  
    signupFields.  
    map(fi => getSingleton.getActualBaseField(user, fi)).  
    filter(f => !ignorePassword || (f match {  
          case f: MappedPassword[User] => false  
          case _ => true  
        })).  
    flatMap(f =>  
      f.toForm.toList.map(form =>  
        (<div class="clearfix"><label>{f.displayName}</label><div class="input">{form}</div></div>) ) )  
  } 
}

class User extends MegaProtoUser[User] 
           with OneToMany[Long, User] 
           with ManyToMany {
  
  def getSingleton = User
  
  object userName extends MappedString(this, 32) {
    override def dbIndexed_? = true  
    override def validations = valUnique(S.??("unique.username")) _ :: super.validations
    override def required_? = true
    override def displayName = ?("username")
  }
  
  object registerDate extends MappedDateTime(this) {
    override def defaultValue = new java.util.Date
  }
  
  object lastLogin extends MappedDateTime(this)

  object about extends MappedTextarea(this, 2048) {
    override def textareaRows  = 5
    override def textareaCols = 50
    override def displayName = "About"
  }
  
  object userSite extends MappedString(this,128) {
    override def displayName = "WWW"
  }
  
  object moderator extends MappedBoolean(this) {
    override def defaultValue = false
  }
  
  object businessAccount extends MappedBoolean(this) {
    override def defaultValue = false
  }
  
  object facebookProfile extends MappedString(this, 128)
  object fbuid extends MappedInt(this)
  
  object twitterProfile extends MappedString(this, 128) 
  object twuid extends MappedInt(this)
  
  object location extends MappedString(this, 128)
  
  object deals extends MappedOneToMany(Deal, Deal.userid, 
    OrderBy(Deal.start, Descending))

  object loginReferer extends SessionVar("/") 
  
  def loginAndRedirectURL = "/user/login?next=" + S.uri
  
  override def niceName: String = 
    (firstName.is, lastName.is, email.is) match {
      case (f, l, _) if f.length > 1 && l.length > 1 => f+" "+l  
      case (f, _, _) if f.length > 1 => f  
      case (_, l, _) if l.length > 1 => l  
      case (_, _, e) => e  
    }
  
  def getUsernameById(userId: Long): String = 
    User.find(By(User.id, userId)) match {
      case Full(user) => user.userName.toString
      case _ => "Konto usunięte"
    }
  
  def withIdExist_?(userId: Long): Boolean = 
    User.find(By(User.id, userId)) match {
      case Full(user) => true
      case _ => false
    }
  
  def uniqueEmail_?(email: String): Boolean = 
    User.find(By(User.email, email)) match {
      case Full(user) => false
      case _ => true
    }
  
  def uniqueUserName_?(userName: String): Boolean =
    User.find(By(User.userName, userName)) match {
      case Full(user) => false
      case _ => true
    }
}

}
}
