import _root_.db.ModelService
import discourse.OreDiscourseApi
import javax.inject._
import models.viewhelper.HeaderData
import ore.{OreConfig, OreEnv}
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

/** A custom server error handler */
class ErrorHandler @Inject()(env: Environment,
                             conf: Configuration,
                             sourceMapper: OptionalSourceMapper,
                             router: Provider[Router],
                             implicit val oreEnv: OreEnv,
                             implicit val config: OreConfig,
                             implicit val service: ModelService,
                             implicit val forums: OreDiscourseApi,
                             override val messagesApi: MessagesApi)
                             extends DefaultHttpErrorHandler(env, conf, sourceMapper, router)
                               with I18nSupport {

  override def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    implicit val session: Session = request.session
    implicit val requestImpl: RequestHeader = request
    implicit val headerData: HeaderData = HeaderData() // Empty HeaderData on error
    implicit val flash: Flash = request.flash

    Future.successful {
      if (exception.cause.isInstanceOf[TimeoutException])
        GatewayTimeout(views.html.errors.timeout())
      else
        InternalServerError(views.html.errors.error())
    }
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    implicit val session: Session = request.session
    implicit val requestImpl: RequestHeader = request
    implicit val headerData: HeaderData = HeaderData() // Empty HeaderData on error
    implicit val flash: Flash = request.flash

    Future.successful(NotFound(views.html.errors.notFound()))
  }

}
