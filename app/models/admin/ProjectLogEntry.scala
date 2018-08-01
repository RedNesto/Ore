package models.admin

import java.sql.Timestamp

import db.Model
import db.impl.ProjectLogEntryTable

/**
  * Represents an entry in a [[ProjectLog]].
  *
  * @param id               Unique ID
  * @param createdAt        Instant of creation
  * @param logId            ID of log
  * @param tag              Entry tag
  * @param message          Entry message
  * @param occurrences      Amount of occurrences this entry has had
  * @param lastOccurrence   Instant of last occurrence
  */
case class ProjectLogEntry(id: Option[Int] = None,
                           createdAt: Option[Timestamp] = None,
                           logId: Int,
                           tag: String,
                           message: String,
                           occurrences: Int = 1,
                           lastOccurrence: Timestamp) extends Model {

  override type T = ProjectLogEntryTable
  override type M = ProjectLogEntry

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): ProjectLogEntry = this.copy(id = id, createdAt = theTime)

}
