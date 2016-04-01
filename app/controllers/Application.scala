package controllers

import javax.inject.Inject

import auth.DiscourseSSO._
import db.Storage
import models.auth.{FakeUser, User}
import models.project.Categories
import models.project.Categories.Category
import play.api.Play
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import routes.{Application => self}
import views.{html => views}

import scala.util.{Failure, Success}

class Application @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index(categories: Option[String]) = Action { implicit request =>
    categories match {
      case None => Storage.now(Storage.getProjects) match {
        case Failure(thrown) => throw thrown
        case Success(projects) => Ok(views.index(projects, None))
      }
      case Some(csv) =>
        val categoryArray = Categories.fromString(csv)
        Storage.now(Storage.getProjects(categoryArray.map(_.id))) match {
          case Failure(thrown) => throw thrown
          case Success(projects) => Ok(views.index(projects, Some(categoryArray)))
        }
    }
  }

  /**
    * Redirect to forums for SSO authentication and then back here again.
    *
    * @param sso  Incoming payload from forums
    * @param sig  Incoming signature from forums
    * @return     Logged in home
    */
  def logIn(sso: Option[String], sig: Option[String]) = Action {
    if (FakeUser.ENABLED) {
      Redirect(self.index(None)).withSession(Security.username -> FakeUser.username, "email" -> FakeUser.email)
    } else if (sso.isEmpty || sig.isEmpty) {
      Redirect(getRedirect)
    } else {
      val userData = authenticate(sso.get, sig.get)
      var user = new User(userData._1, userData._2, userData._3, userData._4)
      user = Storage.findOrCreateUser(user)
      Redirect(self.index(None)).withSession(Security.username -> user.username, "email" -> user.email)
    }
  }

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut = Action { implicit request =>
    Redirect(self.index(None)).withNewSession
  }

}
