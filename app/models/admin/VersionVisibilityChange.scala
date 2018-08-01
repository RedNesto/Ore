package models.admin

import java.sql.Timestamp

import db.Model
import db.impl.VersionVisibilityChangeTable
import db.impl.model.common.VisibilityChange
import models.project.Page
import models.user.User
import ore.OreConfig
import play.twirl.api.Html
import util.functional.OptionT
import util.instances.future._

case class VersionVisibilityChange(id: Option[Int] = None,
                            createdAt: Option[Timestamp] = None,
                            createdBy: Option[Int] = None,
                            projectId: Int = -1,
                            comment: String,
                            resolvedAt: Option[Timestamp] = None,
                            resolvedBy: Option[Int] = None,
                            visibility: Int = 1) extends Model with VisibilityChange {
  /** Self referential type */
  override type M = VersionVisibilityChange
  /** The model's table */
  override type T = VersionVisibilityChangeTable

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
