package models.user

import java.sql.Timestamp

import db.Model
import db.impl.SignOnTable

/**
  * Represents a sign-on instance for a user.
  *
  * @param id           User ID
  * @param createdAt    Instant of creation
  * @param nonce        Nonce used
  * @param isCompleted  True if sign on was completed
  */
case class SignOn(id: Option[Int] = None,
                  createdAt: Option[Timestamp] = None,
                  nonce: String,
                  isCompleted: Boolean = false) extends Model {

  override type M = SignOn
  override type T = SignOnTable

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): SignOn = this.copy(id = id, createdAt = theTime)
}
