package models.admin

import java.sql.Timestamp

import db.Model
import models.project.Page
import models.user.User
import util.functional.OptionT
import util.instances.future._
import play.twirl.api.Html
import scala.concurrent.{ExecutionContext, Future}

import db.impl.ProjectVisibilityChangeTable
import db.impl.model.common.VisibilityChange
import ore.OreConfig

case class ProjectVisibilityChange(id: Option[Int] = None,
                            createdAt: Option[Timestamp] = None,
                            createdBy: Option[Int] = None,
                            projectId: Int = -1,
                            comment: String,
                            resolvedAt: Option[Timestamp] = None,
                            resolvedBy: Option[Int] = None,
                            visibility: Int = 1) extends Model with VisibilityChange {
  /** Self referential type */
  override type M = ProjectVisibilityChange
  /** The model's table */
  override type T = ProjectVisibilityChangeTable

  /** Render the comment as Html */
  def renderComment(implicit config: OreConfig): Html = Page.render(comment)

  /**
    * Returns a copy of this model with an updated ID and timestamp.
    *
    * @param id      ID to set
    * @param theTime Timestamp
    * @return Copy of model
    */
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = createdAt)
}
