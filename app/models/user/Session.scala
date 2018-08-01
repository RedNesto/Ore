package models.user

import java.sql.Timestamp

import db.impl.SessionTable
import db.impl.access.UserBase
import db.{Expirable, Model}
import scala.concurrent.{ExecutionContext, Future}

import security.spauth.SpongeAuthApi
import util.functional.OptionT

/**
  * Represents a persistant [[User]] session.
  *
  * @param id         Unique ID
  * @param createdAt  Instant of creation
  * @param expiration Instant of expiration
  * @param username   Username session belongs to
  * @param token      Unique token
  */
case class Session(id: Option[Int] = None,
                   createdAt: Option[Timestamp] = None,
                   expiration: Timestamp,
                   username: String,
                   token: String) extends Model with Expirable {

  override type M = Session
  override type T = SessionTable

  /**
    * Returns the [[User]] that this Session belongs to.
    *
    * @param users UserBase instance
    * @return User session belongs to
    */
  def user(implicit users: UserBase, ec: ExecutionContext, auth: SpongeAuthApi): OptionT[Future, User] = users.withName(this.username)

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
