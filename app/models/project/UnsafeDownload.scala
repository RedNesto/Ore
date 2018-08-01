package models.project

import java.sql.Timestamp

import com.github.tminglei.slickpg.InetString

import db.Model
import db.impl.UnsafeDownloadsTable
import ore.project.io.DownloadTypes.DownloadType

/**
  * Represents a download instance of an unreviewed [[Project]] [[Version]].
  *
  * @param id           Unique ID
  * @param createdAt    Instant of creation
  * @param userId       User who downloaded (if applicable)
  * @param address      Address of client
  * @param downloadType Type of download
  */
case class UnsafeDownload(id: Option[Int] = None,
                          createdAt: Option[Timestamp] = None,
                          userId: Option[Int] = None,
                          address: InetString,
                          downloadType: DownloadType) extends Model {

  override type M = UnsafeDownload
  override type T = UnsafeDownloadsTable

  def copyWith(id: Option[Int], theTime: Option[Timestamp]): UnsafeDownload = this.copy(id = id, createdAt = theTime)

}
