package models.project

import java.sql.Timestamp

import db.{Model, ModelService, Named}
import db.access.ModelAccess
import db.impl.ChannelTable
import ore.Colors._
import ore.permission.scope.ProjectScope

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * @param id           Unique identifier
  * @param createdAt    Instant of creation
  * @param isNonReviewed Whether this channel should be excluded from the staff
  *                     approval queue
  * @param name        Name of channel
  * @param color       Color used to represent this Channel
  * @param projectId    ID of project this channel belongs to
  */
case class Channel(id: Option[Int] = None,
                   createdAt: Option[Timestamp] = None,
                   projectId: Int,
                   name: String,
                   color: Color,
                   isNonReviewed: Boolean = false)
                   extends Model
                     with Named
                     with ProjectScope {

  override type T = ChannelTable
  override type M = Channel

  def this(name: String, color: Color, projectId: Int) = this(id = None, name = name, color = color, projectId = projectId)

  def isReviewed: Boolean = !isNonReviewed

  /**
    * Returns all Versions in this channel.
    *
    * @return All versions
    */
  def versions(implicit service: ModelService): ModelAccess[Version] = this.schema.getChildren[Version](classOf[Version], this)

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Channel = this.copy(id = id, createdAt = theTime)
}

object Channel {

  implicit val channelsAreOrdered: Ordering[Channel] = (x: Channel, y: Channel) => x.name.compare(y.name)

  /**
    * The colors a Channel is allowed to have.
    */
  val Colors: Seq[Color] = Seq(Purple, Violet, Magenta, Blue, Aqua, Cyan, Green,
                               DarkGreen, Chartreuse, Amber, Orange, Red)

}
