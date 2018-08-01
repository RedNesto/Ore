package models.user.role

import java.sql.Timestamp

import db.{Model, ModelService}
import db.impl.OrganizationRoleTable
import ore.Visitable
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.OrganizationScope
import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a [[RoleModel]] within an [[models.user.Organization]].
  *
  * @param id             Model ID
  * @param createdAt      Timestamp instant of creation
  * @param userId         ID of User this role belongs to
  * @param organizationId ID of Organization this role belongs to
  * @param roleType      Type of Role
  * @param isAccepted    True if has been accepted
  */
case class OrganizationRole(id: Option[Int] = None,
                            createdAt: Option[Timestamp] = None,
                            userId: Int,
                            organizationId: Int = -1,
                            roleType: RoleType,
                            isAccepted: Boolean = false)
                            extends RoleModel with OrganizationScope {

  override type M = OrganizationRole
  override type T = OrganizationRoleTable

  def this(userId: Int, roleType: RoleType) = this(id = None, userId = userId, roleType = roleType)

  override def subject(implicit ec: ExecutionContext, service: ModelService): Future[Visitable] = this.organization
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
