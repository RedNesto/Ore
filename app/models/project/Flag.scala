package models.project

import java.sql.Timestamp
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import db.{Model, ModelService}
import db.impl.FlagTable
import models.user.User
import ore.permission.scope.ProjectScope
import ore.project.FlagReasons.FlagReason
import ore.user.UserOwned

/**
  * Represents a flag on a Project that requires staff attention.
  *
  * @param id           Unique ID
  * @param createdAt    Timestamp instant of creation
  * @param projectId    Project ID
  * @param userId       Reporter ID
  * @param reason       Reason for flag
  * @param isResolved   True if has been reviewed and resolved by staff member
  */
case class Flag(id: Option[Int],
                createdAt: Option[Timestamp],
                projectId: Int,
                userId: Int,
                reason: FlagReason,
                comment: String,
                isResolved: Boolean = false,
                resolvedAt: Option[Timestamp] = None,
                resolvedBy: Option[Int] = None)
                extends Model
                  with UserOwned
                  with ProjectScope {

  override type M = Flag
  override type T = FlagTable

  def this(projectId: Int, userId: Int, reason: FlagReason, comment: String) = {
    this(id = None, createdAt = None, projectId = projectId, userId = userId, reason = reason, comment = comment)
  }

  /**
    * Sets whether this Flag has been marked as resolved.
    *
    * @param resolved True if resolved
    */
  def markResolved(resolved: Boolean, user: Option[User])(implicit ec: ExecutionContext, service: ModelService): Future[Flag] = Defined {
    val (at, by) = if(resolved)
      (Some(Timestamp.from(Instant.now)), Some(user.flatMap(_.id).getOrElse(-1)))
    else
      (None, None)

    service.update(
      copy(
        isResolved = resolved,
        resolvedAt = at,
        resolvedBy = by
      )
    )
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Flag = this.copy(id = id, createdAt = theTime)

}
