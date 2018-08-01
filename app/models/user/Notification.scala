package models.user

import java.sql.Timestamp

import db.Model
import db.impl.NotificationTable
import ore.user.UserOwned
import ore.user.notification.NotificationTypes.NotificationType
import util.instances.future._
import scala.concurrent.{ExecutionContext, Future}

import db.impl.access.UserBase

/**
  * Represents a [[User]] notification.
  *
  * @param id               Unique ID
  * @param createdAt        Instant of cretion
  * @param userId           ID of User this notification belongs to
  * @param notificationType Type of notification
  * @param messageArgs      The unlocalized message to display, with the
  *                         parameters to use when localizing
  * @param action           Action to perform on click
  * @param isRead             True if notification has been read
  */
case class Notification(id: Option[Int] = None,
                        createdAt: Option[Timestamp] = None,
                        userId: Int = -1,
                        originId: Int,
                        notificationType: NotificationType,
                        messageArgs: List[String],
                        action: Option[String] = None,
                        isRead: Boolean = false)
                        extends Model with UserOwned {
  //TODO: Would be neat to have a NonEmptyList to get around guarding against this
  require(messageArgs.nonEmpty, "Notification created with no message arguments")

  override type M = Notification
  override type T = NotificationTable

  /**
    * Returns the [[User]] from which this Notification originated from.
    *
    * @return User from which this originated from
    */
  def origin(implicit ec: ExecutionContext, userBase: UserBase): Future[User] =
    userBase.get(this.originId).getOrElse(throw new NoSuchElementException("Get on None"))

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
