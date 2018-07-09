package models.project

import java.sql.Timestamp
import java.time.Instant

import db.{Named, ObjectId, ObjectReference, ObjectTimestamp}
import db.impl.model.OreModel
import db.impl.table.ModelKeys._
import db.impl.{OrePostgresDriver, TagTable}
import db.table.MappedType
import models.project.TagColors.TagColor
import slick.jdbc.JdbcType

case class Tag(override val id: ObjectId = ObjectId.Uninitialized,
               private var _versionIds: List[Int],
               name: String,
               data: String,
               color: TagColor)
  extends OreModel(id, ObjectTimestamp.Uninitialized)
    with Named {

  override val createdAt: ObjectTimestamp = ObjectTimestamp(Timestamp.from(Instant.EPOCH))

  override type M = Tag
  override type T = TagTable

  def versionIds: List[ObjectReference] = this._versionIds

  def addVersionId(versionId: ObjectReference) = {
    this._versionIds = this._versionIds :+ versionId
    if (isDefined) {
      update(TagVersionIds)
    }
  }

  def copyWith(id: ObjectId, theTime: ObjectTimestamp): Tag = this.copy(id = id)
}

object TagColors extends Enumeration {

  // Tag colors
  val Sponge = TagColor(1, "#F7Cf0D", "#000000")
  val Forge = TagColor(2, "#910020", "#FFFFFF")
  val Unstable = TagColor(3, "#FFDAB9", "#000000")

  def withId(id: Int): TagColor = {
    this.apply(id).asInstanceOf[TagColor]
  }

  /** Represents a color. */
  case class TagColor(i: Int, background: String, foreground: String) extends super.Val(i, s"$background $foreground") with MappedType[TagColor] {
    implicit val mapper: JdbcType[TagColor] = OrePostgresDriver.api.tagColorTypeMapper
  }

  implicit def convert(value: Value): TagColor = value.asInstanceOf[TagColor]

}
