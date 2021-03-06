package ore.project

import db.impl.access.ProjectBase
import models.project.{Project, Version}
import models.user.Notification
import ore.user.notification.NotificationTypes
import scala.concurrent.ExecutionContext

/**
  * Notifies all [[models.user.User]]s that are watching the specified
  * [[Version]]'s [[models.project.Project]] that a new Version has been
  * released.
  *
  * @param version  New version
  * @param projects ProjectBase instance
  */
case class NotifyWatchersTask(version: Version, project: Project)(implicit projects: ProjectBase, ec: ExecutionContext)
  extends Runnable {

  def run(): Unit = {
    val notification = Notification(
      originId = project.ownerId,
      notificationType = NotificationTypes.NewProjectVersion,
      messageArgs = List("notification.project.newVersion", project.name, version.name),
      action = Some(version.url(project))
    )

    for {
      watchers <- project.watchers.all
    } yield {
      for (watcher <- watchers.filterNot(_.userId == version.authorId))
        watcher.sendNotification(notification)
    }

  }

}
