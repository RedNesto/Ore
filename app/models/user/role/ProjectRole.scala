package models.user.role

import java.sql.Timestamp

import db.impl.ProjectRoleTable
import ore.Visitable
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ProjectScope

import scala.concurrent.{ExecutionContext, Future}
import db.ModelService

/**
  * Represents a [[ore.project.ProjectMember]]'s role in a
  * [[models.project.Project]]. A ProjectRole determines what a Member can and
  * cannot do within a [[ProjectScope]].
  *
  * @param id         Model ID
  * @param createdAt  Timestamp instant of creation
  * @param userId     ID of User this role belongs to
  * @param roleType   Type of role
  * @param projectId  ID of project this role belongs to
  */
case class ProjectRole(id: Option[Int] = None,
                       createdAt: Option[Timestamp] = None,
                       userId: Int,
                       projectId: Int,
                       roleType: RoleType,
                       isAccepted: Boolean = false)
                       extends RoleModel
                         with ProjectScope {

  override type M = ProjectRole
  override type T = ProjectRoleTable

  def this(userId: Int, roleType: RoleType, projectId: Int, accepted: Boolean, visible: Boolean) = this(
    id = None,
    createdAt = None,
    userId = userId,
    roleType = roleType,
    projectId = projectId,
    isAccepted = accepted
  )

  override def subject(implicit ec: ExecutionContext, service: ModelService): Future[Visitable] = this.project
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): ProjectRole = this.copy(id = id, createdAt = theTime)

}
