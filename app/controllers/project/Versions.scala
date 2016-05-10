package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Versions => self}
import db.ModelService
import db.impl.OrePostgresDriver.api._
import form.OreForms
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import ore.UserBase
import ore.permission.{EditVersions, ReviewProjects}
import ore.project.ProjectBase
import ore.project.util.{InvalidPluginFileException, PendingProject, ProjectFactory, ProjectFileManager}
import ore.statistic.StatTracker
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import util.OreConfig
import util.StringUtils.equalsIgnoreCase
import views.html.projects.{versions => views}

import scala.util.{Failure, Success}

/**
  * Controller for handling Version related actions.
  */
class Versions @Inject()(override val messagesApi: MessagesApi,
                         val stats: StatTracker,
                         val forms: OreForms,
                         implicit val fileManager: ProjectFileManager,
                         implicit val config: OreConfig,
                         implicit val cacheApi: CacheApi,
                         implicit val projectFactory: ProjectFactory,
                         implicit val ws: WSClient,
                         implicit override val users: UserBase,
                         implicit override val projects: ProjectBase,
                         implicit override val forums: DiscourseApi,
                         implicit override val service: ModelService) extends BaseController {

  private def VersionEditAction(author: String, slug: String)
  = AuthedProjectAction(author, slug) andThen ProjectPermissionAction(EditVersions)

  /**
    * Shows the specified version view page.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return Version view
    */
  def show(author: String, slug: String, versionString: String) = {
    ProjectAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        stats.projectViewed { implicit request =>
          Ok(views.view(project, version.channel, version))
        }
      }
    }
  }

  /**
    * Saves the specified Version's description.
    *
    * @param author        Project owner
    * @param slug          Project slug
    * @param versionString Version name
    * @return View of Version
    */
  def saveDescription(author: String, slug: String, versionString: String) = {
    VersionEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        version.description = forms.VersionDescription.bindFromRequest.get.trim
        Redirect(self.show(author, slug, versionString))
      }
    }
  }

  /**
    * Sets the specified Version as the recommended download.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param versionString  Version name
    * @return               View of version
    */
  def setRecommended(author: String, slug: String, versionString: String) = {
    VersionEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        project.recommendedVersion = version
        Redirect(self.show(author, slug, versionString))
      }
    }
  }

  /**
    * Sets the specified Version as approved by the moderation staff.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param versionString  Version name
    * @return               View of version
    */
  def approve(author: String, slug: String, versionString: String) = {
    (AuthedProjectAction(author, slug) andThen ProjectPermissionAction(ReviewProjects)) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        version.setReviewed(reviewed = true)
        Redirect(self.show(author, slug, versionString))
      }
    }
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @param channels Visible channels
    * @return View of project
    */
  def showList(author: String, slug: String, channels: Option[String]) = {
    ProjectAction(author, slug) { implicit request =>
      val project = request.project
      val allChannels = project.channels.toSeq

      var visibleNames: Option[Array[String]] = channels.map(_.toLowerCase.split(','))
      val visible: Option[Array[Channel]] = visibleNames.map(_.map { name =>
        allChannels.find(_.name.equalsIgnoreCase(name)).get
      })
      val visibleIds: Array[Int] = visible.map(_.map(_.id.get)).getOrElse(allChannels.map(_.id.get).toArray)

      val load = config.projects.getInt("init-version-load").get
      val versions = project.versions.sorted(_.createdAt.desc, _.channelId inSetBind visibleIds, load)
      if (visibleNames.isDefined && visibleNames.get.toSet.equals(allChannels.map(_.name.toLowerCase).toSet)) {
        visibleNames = None
      }

      stats.projectViewed { implicit request =>
        Ok(views.list(project, allChannels, versions, visibleNames))
      }
    }
  }

  /**
    * Shows the creation form for new versions on projects.
    *
    * @param author Owner of project
    * @param slug   Project slug
    * @return Version creation view
    */
  def showCreator(author: String, slug: String) = {
    VersionEditAction(author, slug) { implicit request =>
      val project = request.project
      Ok(views.create(project, None, Some(project.channels.values.toSeq), showFileControls = true))
    }
  }

  /**
    * Uploads a new version for a project for further processing.
    *
    * @param author Owner name
    * @param slug   Project slug
    * @return Version create page (with meta)
    */
  def upload(author: String, slug: String) = {
    VersionEditAction(author, slug) { implicit request =>
      request.body.asMultipartFormData.get.file("pluginFile") match {
        case None => Redirect(self.showCreator(author, slug)).flashing("error" -> "Missing file")
        case Some(tmpFile) =>
          // Initialize plugin file
          projectFactory.initUpload(tmpFile.ref, tmpFile.filename, request.user) match {
            case Failure(thrown) => if (thrown.isInstanceOf[InvalidPluginFileException]) {
              // PEBKAC
              Redirect(self.showCreator(author, slug))
                .flashing("error" -> "Invalid plugin file.")
            } else {
              throw thrown
            }
            case Success(plugin) =>
              val project = request.project
              // Check plugin ID
              if (!plugin.meta.get.getId.equals(project.pluginId)) {
                Redirect(self.showCreator(author, slug))
                  .flashing("error" -> "The uploaded plugin ID must match your project's plugin ID.")
              } else {
                // Create version from meta file
                val version = Version.fromMeta(project, plugin)
                if (version.exists && config.projects.getBoolean("file-validate").get) {
                  Redirect(self.showCreator(author, slug))
                    .flashing("error" -> "Found a duplicate file in project. Plugin files may only be uploaded once.")
                } else {

                  // Get first channel for default
                  val channelName: String = project.channels.values.head.name

                  // Cache for later use
                  Version.setPending(author, slug, channelName, version, plugin)
                  Redirect(self.showCreatorWithMeta(author, slug, version.versionString))
                }
              }
          }
      }
    }
  }

  /**
    * Displays the "version create" page with the associated plugin meta-data.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return Version create view
    */
  def showCreatorWithMeta(author: String, slug: String, versionString: String) = {
    Authenticated { implicit request =>
      // Get pending version
      Version.getPending(author, slug, versionString) match {
        case None => Redirect(self.showCreator(author, slug))
        case Some(pendingVersion) =>
          // Get project
          pendingOrReal(author, slug) match {
            case None => Redirect(self.showCreator(author, slug))
            case Some(p) => p match {
              case pending: PendingProject =>
                Ok(views.create(pending.project, Some(pendingVersion), None, showFileControls = false))
              case real: Project =>
                Ok(views.create(real, Some(pendingVersion), Some(real.channels.toSeq), showFileControls = true))
            }
          }
      }
    }
  }

  private def pendingOrReal(author: String, slug: String): Option[Any] = {
    // Returns either a PendingProject or existing Project
    projects.withSlug(author, slug) match {
      case None => projects.getPending(author, slug)
      case Some(project) => Some(project)
    }
  }

  /**
    * Completes the creation of the specified pending version or project if
    * first version.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return New version view
    */
  def create(author: String, slug: String, versionString: String) = {
    Authenticated { implicit request =>
      // First get the pending Version
      Version.getPending(author, slug, versionString) match {
        case None => Redirect(self.showCreator(author, slug)) // Not found
        case Some(pendingVersion) =>
          // Get submitted channel
          forms.VersionCreate.bindFromRequest.fold(
            hasErrors => Redirect(self.showCreatorWithMeta(author, slug, versionString))
              .flashing("error" -> hasErrors.errors.head.message),

            versionData => {
              // Channel is valid
              pendingVersion.channelName = versionData.channelName.trim
              pendingVersion.channelColor = versionData.color

              // Check for pending project
              projects.getPending(author, slug) match {
                case None =>
                  // No pending project, create version for existing project
                  withProject(author, slug) { project =>
                    val existingChannel = project.channels.find {
                      equalsIgnoreCase(_.name, pendingVersion.channelName)
                    }.orNull

                    var channelResult: Either[String, Channel] = Right(existingChannel)
                    if (existingChannel == null) channelResult = versionData.addTo(project)
                    channelResult.fold(
                      error => Redirect(self.showCreatorWithMeta(author, slug, versionString))
                        .flashing("error" -> error),
                      channel => {
                        val newVersion = pendingVersion.complete.get
                        if (versionData.recommended) project.recommendedVersion = newVersion
                        Redirect(self.show(author, slug, versionString))
                      }
                    )
                  }
                case Some(pendingProject) =>
                  // Found a pending project, create it with first version
                  pendingProject.complete.get
                  Redirect(routes.Projects.show(author, slug))
              }
            }
          )
      }
    }
  }

  /**
    * Deletes the specified version and returns to the version page.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return Versions page
    */
  def delete(author: String, slug: String, versionString: String) = {
    VersionEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        version.delete()
        Redirect(self.showList(author, slug, None))
      }
    }
  }

  /**
    * Sends the specified Project Version to the client.
    *
    * @param author        Project owner
    * @param slug          Project slug
    * @param versionString Version string
    * @return Sent file
    */
  def download(author: String, slug: String, versionString: String) = {
    ProjectAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        stats.versionDownloaded(version) { implicit request =>
          Ok.sendFile(fileManager.uploadPath(author, project.name, versionString).toFile)
        }
      }
    }
  }

  /**
    * Sends the specified project's current recommended version to the client.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Sent file
    */
  def downloadRecommended(author: String, slug: String) = {
    ProjectAction(author, slug) { implicit request =>
      val project = request.project
      val rv = project.recommendedVersion
      stats.versionDownloaded(rv) { implicit request =>
        Ok.sendFile(fileManager.uploadPath(author, project.name, rv.versionString).toFile)
      }
    }
  }

}
