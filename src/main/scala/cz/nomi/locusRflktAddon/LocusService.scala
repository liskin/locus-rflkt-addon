/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{Context, Intent, IntentFilter}

import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.{LocusConst, LocusUtils}
import locus.api.android.ActionTools
import LocusUtils.LocusVersion
import locus.api.objects.extra.ExtraData

import Log._
import Broadcasts._

trait LocusApi {
  def toggleRecording(): Unit
}

trait LocusService extends RService
  with LocalService[LocusService] with LocusApi
{ this: RflktApi =>

  import org.scaloid.common.toast

  onRegister {
    logger.info(s"LocusService: onRegister")
    enablePeriodicUpdatesReceiver()
  }

  onUnregister {
    logger.info(s"LocusService: onUnregister")
    disablePeriodicUpdatesReceiver()
  }

  broadcastReceiver(LocusConst.ACTION_PERIODIC_UPDATE) { (context: Context, intent: Intent) =>
    logger.info(s"periodic update received")
    PeriodicUpdatesHandler.getInstance.onReceive(context, intent, OnUpdate)
  }

  private lazy val locusVer: LocusVersion = {
    val ver = LocusUtils.getActiveVersion(this)
    val locusInfo = ActionTools.getLocusInfo(this, ver)
    if (!locusInfo.isPeriodicUpdatesEnabled) {
      toast("periodic updates in Locus disabled :-(")
    }
    ver
  }

  private def enablePeriodicUpdatesReceiver() {
    logger.info("enablePeriodicUpdatesReceiver")
    ActionTools.enablePeriodicUpdatesReceiver(this, locusVer, classOf[PeriodicUpdateReceiver])
  }

  private def disablePeriodicUpdatesReceiver() {
    logger.info("disablePeriodicUpdatesReceiver")
    ActionTools.disablePeriodicUpdatesReceiver(this, locusVer, classOf[PeriodicUpdateReceiver])
  }

  private object OnUpdate extends PeriodicUpdatesHandler.OnUpdate {
    import LocusUtils.LocusVersion
    import RflktApi.{Val, Str, Vis}

    def onIncorrectData() {
      // TODO: log something
    }

    def onUpdate(version: LocusVersion, update: UpdateContainer) {
      // TODO: move to the end, maybe skip some updates
      lastUpdate = Some(update)

      val loc = update.getLocMyLocation()
      val curSpeed = Option(loc.getSpeed()).filter(_ != 0).map(_ * 36 / 10)
      val curHeartRate = Option(loc.getSensorHeartRate()).filter(_ != 0)
      val curCadence = Option(loc.getSensorCadence()).filter(_ != 0)
      val current = Seq(
        "SPEED_CURRENT.value" -> formatFloatFixed(curSpeed),
        "BIKE_CAD_CURRENT.value" -> formatInt(curCadence),
        "HR_CURRENT.value" -> formatInt(curHeartRate)
      )

      val trackRecord = Option(update.getTrackRecordContainer())
      //val avgSpeed = trackRecord.map(_.getSpeedAvg()).filter(_ != 0).map(_ * 36 / 10)
      val distance = trackRecord.map(_.getDistance() / 1000)
      val workout = Seq(
        //"SPEED_WORKOUT_AV.value" -> formatFloatFixed(avgSpeed),
        "DISTANCE_WORKOUT.value" -> formatDoubleFixed(distance)
      )

      val now = java.util.Calendar.getInstance().getTime()
      val recStatus = trackRecord.map(tr =>
          if (tr.isTrackRecPaused()) "hold" else "rec"
      ).getOrElse("")
      val clock = Seq(
        "CLOCK.value" -> formatTime(now),
        "CLOCK.rec_stopped" -> Vis(trackRecord.isEmpty),
        "CLOCK.rec_paused" -> Vis(trackRecord.exists(_.isTrackRecPaused()))
      )

      val guideTrack = Option(update.getGuideTypeTrack())
      val nav1Action = guideTrack.map(_.getNavPoint1Action())
      val nav1Name = guideTrack.flatMap(g => Option(g.getNavPoint1Name()))
      val nav1Dist = guideTrack.map(_.getNavPoint1Dist() / 1000).filter(_ != 0)
      val nav2Action = guideTrack.map(_.getNavPoint2Action())
      val nav2Name = guideTrack.flatMap(g => Option(g.getNavPoint2Name()))
      val nav2Dist = guideTrack.map(_.getNavPoint2Dist() / 1000).filter(_ != 0)
      val nav1Icon = setNavIcon("NAV1_ACTION", nav1Action)
      val nav2Icon = setNavIcon("NAV2_ACTION", nav2Action)
      val nav = Seq(
        "NAV1_NAME.value" -> formatString(nav1Name.map(normalizeString)),
        "NAV1_DIST.value" -> formatDouble(nav1Dist),
        "NAV2_NAME.value" -> formatString(nav2Name.map(normalizeString)),
        "NAV2_DIST.value" -> formatDouble(nav2Dist)
      ) ++ nav1Icon ++ nav2Icon

      setRflkt((clock ++ current ++ workout ++ nav): _*)
    }

    private val timeFormat = new java.text.SimpleDateFormat("HH:mm:ss")

    private def formatString(s: Option[String]): Str = Str(s.getOrElse("--"))

    private def formatTime(t: java.util.Date): Str = Str(timeFormat.format(t))

    private def formatInt(i: Option[Int]): Str =
      formatString(i.map(v => f"$v%d"))

    private def formatFloatFixed(f: Option[Float]): Str =
      formatString(f.map(v => f"$v%.1f"))

    private def formatDoubleFixed(d: Option[Double]): Str =
      formatString(d.map(v => f"$v%.1f"))

    private def formatDouble(d: Option[Double]): Str =
      formatString {
        d.map { v =>
          if (v.abs > 99) {
            f"$v%.0f"
          } else if (v.abs > 9) {
            f"$v%.1f"
          } else {
            f"$v%.2f"
          }
        }
      }

    private def normalizeString(s: String): String = {
      import java.text.Normalizer
      val split = Normalizer.normalize(s, Normalizer.Form.NFD)
      "\\p{M}".r.replaceAllIn(split, "")
    }

    private def setNavIcon(group: String, action: Option[Int]): Seq[(String, Val)] = {
      val visibleIcon = action.map(LocusService.actionIcons)
      LocusService.navIcons.map { icon =>
        (s"$group.$icon", Vis(visibleIcon.exists(_ == icon)))
      }
    }
  }

  private var lastUpdate: Option[UpdateContainer] = None

  def toggleRecording() {
    lastUpdate match {
      case Some(u) if u.isTrackRecRecording()
                   && !u.getTrackRecordContainer().isTrackRecPaused() =>
        ActionTools.actionTrackRecordPause(this, locusVer)
      case _ =>
        ActionTools.actionTrackRecordStart(this, locusVer)
    }
  }
}

object LocusService {
  import ExtraData._

  private lazy val actionIcons: Map[Int, String] = Map(
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
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_1 -> "nav_roundabout_1",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_2 -> "nav_roundabout_2",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_3 -> "nav_roundabout_3",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_4 -> "nav_roundabout_4",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_5 -> "nav_roundabout_5",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_6 -> "nav_roundabout_6",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_7 -> "nav_roundabout_7",
    VALUE_RTE_ACTION_ROUNDABOUT_EXIT_8 -> "nav_roundabout_8",
    VALUE_RTE_ACTION_PASS_PLACE -> "nav_waypoint"
  )

  private lazy val navIcons: Seq[String] = actionIcons.values.toSeq.distinct
}
