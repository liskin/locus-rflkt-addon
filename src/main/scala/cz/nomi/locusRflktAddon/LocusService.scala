/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{Context, Intent}

import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.{LocusConst, LocusUtils}
import locus.api.android.ActionTools
import LocusUtils.LocusVersion
import locus.api.objects.extra.ExtraData

import com.wahoofitness.connector.capabilities.Rflkt.ButtonPressType
import com.wahoofitness.common.display.DisplayConfiguration

import Log._
import Broadcasts._

trait LocusService extends RService with RflktApi {
  onRegister {
    enablePeriodicUpdatesReceiver()
  }

  onUnregister {
    disablePeriodicUpdatesReceiver()
  }

  broadcastReceiver(LocusConst.ACTION_PERIODIC_UPDATE)
  { (context: Context, intent: Intent) =>
    logger.info(s"periodic update received")
    PeriodicUpdatesHandler.getInstance.onReceive(context, intent, OnUpdate)
  }

  private lazy val locusVer: Option[LocusVersion] =
    Option(LocusUtils.getActiveVersion(this))

  def isLocusInstalled: Boolean = locusVer.isDefined

  def isLocusPeriodicUpdatesEnabled: Boolean =
    locusVer.map(
      ActionTools.getLocusInfo(this, _).isPeriodicUpdatesEnabled
    ).getOrElse(false)

  def launchLocus() = locusVer.foreach { lv =>
    startActivity(getPackageManager().getLaunchIntentForPackage(lv.getPackageName()))
  }

  private def enablePeriodicUpdatesReceiver() {
    logger.info("enablePeriodicUpdatesReceiver")
    locusVer.foreach(ActionTools.enablePeriodicUpdatesReceiver(this, _, classOf[PeriodicUpdateReceiver]))
  }

  private def disablePeriodicUpdatesReceiver() {
    logger.info("disablePeriodicUpdatesReceiver")
    locusVer.foreach(ActionTools.disablePeriodicUpdatesReceiver(this, _, classOf[PeriodicUpdateReceiver]))
  }

  private object OnUpdate extends PeriodicUpdatesHandler.OnUpdate {
    import LocusUtils.LocusVersion
    import RflktApi.{Val, Vis, Str}
    import display.Const.{Widget => W}

    def onIncorrectData() {
      // TODO: log something
    }

    def onUpdate(version: LocusVersion, update: UpdateContainer) {
      lastUpdate = Some(update)

      import Formatters._

      val now = java.util.Calendar.getInstance().getTime()
      val clock = Seq(
        s"${W.clock}.value" -> formatTime(now)
      )

      val loc = update.getLocMyLocation()
      val curSpeed = Option(loc.getSpeedToDisplay()).filter(_ != 0).map(_ * 36 / 10)
      val curElevation = Option(loc.getAltitude()).filter(_ != 0)
      val curHeartRate = Option(loc.getSensorHeartRate()).filter(_ != 0)
      val curCadence = Option(loc.getSensorCadence()).filter(_ != 0)
      val current = Seq(
        s"${W.speedCurrent}.value" -> formatFloatFixed(curSpeed),
        s"${W.elevationCurrent}.value" -> formatDoubleRound(curElevation),
        s"${W.cadenceCurrent}.value" -> formatInt(curCadence),
        s"${W.heartRateCurrent}.value" -> formatInt(curHeartRate)
      )

      val trackRecord = Option(update.getTrackRecordContainer())
      val time = trackRecord.map(_.getTime() / 1000)
      val timeMoving = trackRecord.map(_.getTimeMove() / 1000)
      val avgSpeed = trackRecord.map(_.getSpeedAvg()).filter(_ != 0).map(_ * 36 / 10)
      val avgMovingSpeed = trackRecord.map(_.getSpeedAvgMove()).filter(_ != 0).map(_ * 36 / 10)
      val maxSpeed = trackRecord.map(_.getSpeedMax()).filter(_ != 0).map(_ * 36 / 10)
      val distanceUphill = trackRecord.map(_.getDistanceUphill() / 1000)
      val distanceDownhill = trackRecord.map(_.getDistanceDownhill() / 1000)
      val elevationUphill = trackRecord.map(_.getAltitudeUphill())
      val elevationDownhill = trackRecord.map(_.getAltitudeDownhill())
      val trackStats = Option(update.getTrackRecStats())
      val distance = trackStats.map(_.getEleTotalAbsDistance() / 1000)
      val workout = Seq(
        s"${W.statusWorkout}.rec_stopped" -> Vis(trackRecord.isEmpty),
        s"${W.statusWorkout}.rec_paused" -> Vis(trackRecord.exists(_.isTrackRecPaused())),
        s"${W.timeWorkout}.value" -> formatDuration(time),
        s"${W.timeMovingWorkout}.value" -> formatDuration(timeMoving),
        s"${W.averageSpeedWorkout}.value" -> formatFloatFixed(avgSpeed),
        s"${W.averageMovingSpeedWorkout}.value" -> formatFloatFixed(avgMovingSpeed),
        s"${W.maxSpeedWorkout}.value" -> formatFloatFixed(maxSpeed),
        s"${W.distanceWorkout}.value" -> formatFloatFixed(distance),
        s"${W.distanceUphillWorkout}.value" -> formatDoubleFixed(distanceUphill),
        s"${W.distanceDownhillWorkout}.value" -> formatDoubleFixed(distanceDownhill),
        s"${W.elevationUphillWorkout}.value" -> formatFloatRound(elevationUphill),
        s"${W.elevationDownhillWorkout}.value" -> formatFloatRound(elevationDownhill)
      )

      val guideTrack = Option(update.getGuideTypeTrack())
      val nav1Action = guideTrack.map(_.getNavPoint1Action())
      val nav1Name = guideTrack.flatMap(g => Option(g.getNavPoint1Name()))
      val nav1Dist = guideTrack.map(_.getNavPoint1Dist() / 1000).filter(_ != 0)
      val nav2Action = guideTrack.map(_.getNavPoint2Action())
      val nav2Name = guideTrack.flatMap(g => Option(g.getNavPoint2Name()))
      val nav2Dist = guideTrack.map(_.getNavPoint2Dist() / 1000).filter(_ != 0)
      val nav1Icon = setNavIcon(W.nav1Action, nav1Action)
      val nav2Icon = setNavIcon(W.nav2Action, nav2Action)
      val nav = Seq(
        s"${W.nav1Name}.value" -> formatString(nav1Name.map(normalizeString)),
        s"${W.nav1Dist}.value" -> formatDouble(nav1Dist),
        s"${W.nav2Name}.value" -> formatString(nav2Name.map(normalizeString)),
        s"${W.nav2Dist}.value" -> formatDouble(nav2Dist)
      ) ++ nav1Icon ++ nav2Icon

      setRflkt((clock ++ current ++ workout ++ nav): _*)
    }

    private def setNavIcon(group: String, action: Option[Int]): Seq[(String, Val)] = {
      val (actionIcons, navIcons) = if (navReduced)
        (LocusService.reducedActionIcons, LocusService.reducedNavIcons)
      else
        (LocusService.fullActionIcons, LocusService.fullNavIcons)

      val visibleIcon = action.map(actionIcons)
      val icons = navIcons.toSeq.map { icon =>
        s"$group.$icon" -> Vis(visibleIcon.exists(_ == icon))
      }
      val roundabout = Seq(
        s"$group.roundabout" -> Vis(visibleIcon.exists(_ == "nav_roundabout")),
        s"$group.roundabout" -> Str(action.flatMap(LocusService.roundaboutExit.get).getOrElse(""))
      )

      icons ++ roundabout
    }
  }

  private var lastUpdate: Option[UpdateContainer] = None

  private def toggleRecording() {
    lastUpdate match {
      case Some(u) if u.isTrackRecRecording()
                   && !u.getTrackRecordContainer().isTrackRecPaused() =>
        locusVer.foreach(ActionTools.actionTrackRecordPause(this, _))
      case _ =>
        locusVer.foreach(ActionTools.actionTrackRecordStart(this, _))
    }
  }

  abstract override def onButtonPressed(fun: String, typ: ButtonPressType) {
    import display.Const.{Function => F}
    (fun, typ) match {
      case (F.startStopWorkout, ButtonPressType.SINGLE) =>
        toggleRecording()
      case _ =>
        super.onButtonPressed(fun, typ)
    }
  }

  private var navReduced: Boolean = false

  abstract override def onLoadComplete(conf: DisplayConfiguration) {
    super.onLoadComplete(conf)

    import display.Const.{Page => P, Custom => C}
    navReduced = Option(conf.getPage(P.navigation))
      .flatMap(p => Option(p.getCustom(C.reduced)))
      .map(_.toBoolean).getOrElse(false)
  }
}

object LocusService {
  import ExtraData._

  lazy val fullActionIcons: Map[Int, String] = Map(
    VALUE_RTE_ACTION_NO_MANEUVER -> "nav_unknown",
    VALUE_RTE_ACTION_CONTINUE_STRAIGHT -> "nav_straight",
    VALUE_RTE_ACTION_NO_MANEUVER_NAME_CHANGE -> "nav_unknown",
    VALUE_RTE_ACTION_LEFT_SLIGHT -> "nav_left_1",
    VALUE_RTE_ACTION_LEFT -> "nav_left_2",
    VALUE_RTE_ACTION_LEFT_SHARP -> "nav_left_3",
    VALUE_RTE_ACTION_RIGHT_SLIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_RIGHT -> "nav_right_2",
    VALUE_RTE_ACTION_RIGHT_SHARP -> "nav_right_3",
    VALUE_RTE_ACTION_STAY_LEFT -> "nav_stay_left",
    VALUE_RTE_ACTION_STAY_RIGHT -> "nav_stay_right",
    VALUE_RTE_ACTION_STAY_STRAIGHT -> "nav_straight",
    VALUE_RTE_ACTION_U_TURN -> "nav_turnaround",
    VALUE_RTE_ACTION_U_TURN_LEFT -> "nav_turnaround",
    VALUE_RTE_ACTION_U_TURN_RIGHT -> "nav_turnaround",
    VALUE_RTE_ACTION_EXIT_LEFT -> "nav_exit_left",
    VALUE_RTE_ACTION_EXIT_RIGHT -> "nav_exit_right",
    VALUE_RTE_ACTION_RAMP_ON_LEFT -> "nav_left_1",
    VALUE_RTE_ACTION_RAMP_ON_RIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_RAMP_STRAIGHT -> "nav_straight",
    VALUE_RTE_ACTION_MERGE_LEFT -> "nav_merge_left",
    VALUE_RTE_ACTION_MERGE_RIGHT -> "nav_merge_right",
    VALUE_RTE_ACTION_MERGE -> "nav_straight",
    VALUE_RTE_ACTION_ENTER_STATE -> "nav_unknown",
    VALUE_RTE_ACTION_ARRIVE_DEST -> "nav_finish",
    VALUE_RTE_ACTION_ARRIVE_DEST_LEFT -> "nav_finish",
    VALUE_RTE_ACTION_ARRIVE_DEST_RIGHT -> "nav_finish",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_1 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_2 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_3 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_4 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_5 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_6 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_7 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_8 -> "nav_roundabout",
    VALUE_RTE_ACTION_PASS_PLACE -> "nav_waypoint"
  )

  lazy val reducedActionIcons: Map[Int, String] = Map(
    VALUE_RTE_ACTION_NO_MANEUVER -> "nav_unknown",
    VALUE_RTE_ACTION_CONTINUE_STRAIGHT -> "nav_straight",
    VALUE_RTE_ACTION_NO_MANEUVER_NAME_CHANGE -> "nav_unknown",
    VALUE_RTE_ACTION_LEFT_SLIGHT -> "nav_left_1",
    VALUE_RTE_ACTION_LEFT -> "nav_left_2",
    VALUE_RTE_ACTION_LEFT_SHARP -> "nav_left_2",
    VALUE_RTE_ACTION_RIGHT_SLIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_RIGHT -> "nav_right_2",
    VALUE_RTE_ACTION_RIGHT_SHARP -> "nav_right_2",
    VALUE_RTE_ACTION_STAY_LEFT -> "nav_left_1",
    VALUE_RTE_ACTION_STAY_RIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_STAY_STRAIGHT -> "nav_straight",
    VALUE_RTE_ACTION_U_TURN -> "nav_turnaround",
    VALUE_RTE_ACTION_U_TURN_LEFT -> "nav_turnaround",
    VALUE_RTE_ACTION_U_TURN_RIGHT -> "nav_turnaround",
    VALUE_RTE_ACTION_EXIT_LEFT -> "nav_left_1",
    VALUE_RTE_ACTION_EXIT_RIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_RAMP_ON_LEFT -> "nav_left_1",
    VALUE_RTE_ACTION_RAMP_ON_RIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_RAMP_STRAIGHT -> "nav_straight",
    VALUE_RTE_ACTION_MERGE_LEFT -> "nav_left_1",
    VALUE_RTE_ACTION_MERGE_RIGHT -> "nav_right_1",
    VALUE_RTE_ACTION_MERGE -> "nav_straight",
    VALUE_RTE_ACTION_ENTER_STATE -> "nav_unknown",
    VALUE_RTE_ACTION_ARRIVE_DEST -> "nav_finish",
    VALUE_RTE_ACTION_ARRIVE_DEST_LEFT -> "nav_finish",
    VALUE_RTE_ACTION_ARRIVE_DEST_RIGHT -> "nav_finish",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_1 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_2 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_3 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_4 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_5 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_6 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_7 -> "nav_roundabout",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_8 -> "nav_roundabout",
    VALUE_RTE_ACTION_PASS_PLACE -> "nav_finish"
  )

  lazy val roundaboutExit: Map[Int, String] = Map(
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_1 -> "1",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_2 -> "2",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_3 -> "3",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_4 -> "4",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_5 -> "5",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_6 -> "6",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_7 -> "7",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_8 -> "8"
  )

  lazy val fullNavIcons: Set[String] = fullActionIcons.values.toSet
  lazy val reducedNavIcons: Set[String] = reducedActionIcons.values.toSet
}
