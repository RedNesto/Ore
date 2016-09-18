package models.user.role

import java.sql.Timestamp

import db.meta.Bind
import ore.Visitable
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ProjectScope

import scala.annotation.meta.field

/**
  * Represents a [[ore.project.ProjectMember]]'s role in a
  * [[models.project.Project]]. A ProjectRole determines what a Member can and
  * cannot do within a [[ProjectScope]].
  *
  * @param id         Model ID
  * @param createdAt  Timestamp instant of creation
  * @param userId     ID of User this role belongs to
  * @param _roleType  Type of role
  * @param projectId  ID of project this role belongs to
  */
case class ProjectRole(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val userId: Int,
                       override val projectId: Int,
                       @(Bind @field) private var _roleType: RoleType,
                       @(Bind @field) private var _isAccepted: Boolean = false)
                       extends RoleModel(id, createdAt, userId, _roleType, _isAccepted)
                         with ProjectScope {

  def this(userId: Int, roleType: RoleType, projectId: Int, accepted: Boolean) = {
    this(id=None, createdAt=None, userId=userId, _roleType=roleType, projectId=projectId, _isAccepted=accepted)
  }

  override def subject: Visitable = this.project
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}