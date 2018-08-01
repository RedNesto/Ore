package ore.project.factory

import db.ModelService
import db.impl.access.ProjectBase
import models.project.{Project, ProjectSettings, Version}
import models.user.role.ProjectRole
import ore.project.io.PluginFile
import ore.{Cacheable, OreConfig}
import play.api.cache.SyncCacheApi
import scala.concurrent.{ExecutionContext, Future}

import discourse.OreDiscourseApi

/**
  * Represents a Project with an uploaded plugin that has not yet been
  * created.
  *
  * @param underlying  Pending project
  * @param file     Uploaded plugin
  */
case class PendingProject(projects: ProjectBase,
                          factory: ProjectFactory,
                          underlying: Project,
                          file: PluginFile,
                          channelName: String,
                          implicit val config: OreConfig,
                          var roles: Set[ProjectRole] = Set(),
                          override val cacheApi: SyncCacheApi)
                         (implicit service: ModelService)
                           extends Cacheable {

  /**
    * The [[Project]]'s internal settings.
    */
  val settings: ProjectSettings = ProjectSettings()

  /**
    * The first [[PendingVersion]] for this PendingProject.
    */
  val pendingVersion: PendingVersion = {
    val version = this.factory.startVersion(this.file, this.underlying, this.settings, this.channelName)
    val model = version.underlying
    version.cache()
    version
  }

  def complete()(implicit ec: ExecutionContext): Future[(Project, Version)] = {
    free()
    for {
      newProject <- this.factory.createProject(this)
      newVersion <- {
        this.pendingVersion.project = newProject
        this.factory.createVersion(this.pendingVersion)
      }
      updatedProject <- service.update(newProject.copy(recommendedVersionId = Some(newVersion._1.id.get)))
    } yield (updatedProject, newVersion._1)
  }

  def cancel()(implicit ec: ExecutionContext, forums: OreDiscourseApi) = {
    free()
    this.file.delete()
    if (this.underlying.isDefined)
      this.projects.delete(this.underlying)
  }

  override def key: String = this.underlying.ownerName + '/' + this.underlying.slug

}
