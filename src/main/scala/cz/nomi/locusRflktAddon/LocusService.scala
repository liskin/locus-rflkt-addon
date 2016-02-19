package cz.nomi.locusRflktAddon

import org.scaloid.common._

import java.text.Normalizer

import android.content.{Context, Intent, IntentFilter}

import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.{LocusConst, LocusUtils}
import locus.api.android.ActionTools
import LocusUtils.LocusVersion
import locus.api.objects.extra.ExtraData

trait LocusApi {
  def toggleRecording(): Unit
}

trait LocusService extends LocalService with Log with LocusApi
{ this: RflktApi =>

  onCreate {
    info(s"LocusService: onCreate")
  }

  onDestroy {
    info(s"LocusService: onDestroy")
  }

  onRegister {
    enablePeriodicUpdatesReceiver()
  }

  onUnregister {
    disablePeriodicUpdatesReceiver()
  }

  broadcastReceiver(LocusConst.ACTION_PERIODIC_UPDATE: IntentFilter) { (context: Context, intent: Intent) =>
    info(s"periodic update received")
    PeriodicUpdatesHandler.getInstance.onReceive(context, intent, OnUpdate)
  }

  private lazy val locusVer: LocusVersion = {
    val ver = LocusUtils.getActiveVersion(ctx)
    val locusInfo = ActionTools.getLocusInfo(ctx, ver)
    if (!locusInfo.isPeriodicUpdatesEnabled) {
      toast("periodic updates in Locus disabled :-(")
    }
    ver
  }

  private def enablePeriodicUpdatesReceiver() {
    info("enablePeriodicUpdatesReceiver")
    ActionTools.enablePeriodicUpdatesReceiver(ctx, locusVer, classOf[PeriodicUpdateReceiver])
  }

  private def disablePeriodicUpdatesReceiver() {
    info("disablePeriodicUpdatesReceiver")
    ActionTools.disablePeriodicUpdatesReceiver(ctx, locusVer, classOf[PeriodicUpdateReceiver])
  }

  private object OnUpdate extends PeriodicUpdatesHandler.OnUpdate {
    import LocusUtils.LocusVersion

    def onIncorrectData() {
      // TODO: log something
    }

    def onUpdate(version: LocusVersion, update: UpdateContainer) {
      lastUpdate = Some(update)

      val now = java.util.Calendar.getInstance().getTime()

      val loc = update.getLocMyLocation()
      val curSpeed = Option(loc.getSpeed()).filter(_ != 0).map(_ * 36 / 10)
      val curHeartRate = Option(loc.getSensorHeartRate()).filter(_ != 0)
      val curCadence = Option(loc.getSensorCadence()).filter(_ != 0)

      val trackRecord = Option(update.getTrackRecordContainer())
      val avgSpeed = trackRecord.map(_.getSpeedAvg()).filter(_ != 0).map(_ * 36 / 10)
      val distance = trackRecord.map(_.getDistance() / 1000)

      val recStatus = trackRecord.map(tr =>
          if (tr.isTrackRecPaused()) "hold" else "rec"
      ).getOrElse("")

      val guideTrack = Option(update.getGuideTypeTrack())
      val distToFinish = guideTrack.map(_.getDistToFinish() / 1000)
      val nav1Action = guideTrack.map(_.getNavPoint1Action())
      val nav1Name = guideTrack.flatMap(g => Option(g.getNavPoint1Name()))
      val nav1Dist = guideTrack.map(_.getNavPoint1Dist() / 1000).filter(_ != 0)
      val nav2Action = guideTrack.map(_.getNavPoint2Action())
      val nav2Name = guideTrack.flatMap(g => Option(g.getNavPoint2Name()))
      val nav2Dist = guideTrack.map(_.getNavPoint2Dist() / 1000).filter(_ != 0)

      setRflkt(
        "CLOCK.value" -> timeFormat.format(now),
        "SPEED_CURRENT.value" -> formatFloatFixed(curSpeed),
        "SPEED_WORKOUT_AV.value" -> formatFloatFixed(avgSpeed),
        "DISTANCE_WORKOUT.value" -> formatDoubleFixed(distance),
        "BIKE_CAD_CURRENT.value" -> formatInt(curCadence),
        "HR_CURRENT.value" -> formatInt(curHeartRate),
        "CLOCK.rec_status" -> recStatus,
        "DISTANCE_TO_FINISH.value" -> formatDoubleFixed(distToFinish),
        "NAV1_ACTION.value" -> formatAction(nav1Action),
        "NAV1_NAME.value" -> formatString(nav1Name.map(normalizeString)),
        "NAV1_DIST.value" -> formatDouble(nav1Dist),
        "NAV2_ACTION.value" -> formatAction(nav2Action),
        "NAV2_NAME.value" -> formatString(nav2Name.map(normalizeString)),
        "NAV2_DIST.value" -> formatDouble(nav2Dist)
      )
    }

    private val timeFormat = new java.text.SimpleDateFormat("HH:mm:ss")

    private def formatString(s: Option[String]): String = s.getOrElse("--")

    private def formatInt(i: Option[Int]): String =
      formatString(i.map(v => f"$v%d"))

    private def formatFloatFixed(f: Option[Float]): String =
      formatString(f.map(v => f"$v%.1f"))

    private def formatDoubleFixed(d: Option[Double]): String =
      formatString(d.map(v => f"$v%.1f"))

    private def formatDouble(d: Option[Double]): String =
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
      val split = Normalizer.normalize(s, Normalizer.Form.NFD)
      "\\p{M}".r.replaceAllIn(split, "")
    }

    private def formatAction(a: Option[Int]): String = formatString(a collect {
      case ExtraData.VALUE_RTE_ACTION_NO_MANEUVER => ""
      case ExtraData.VALUE_RTE_ACTION_CONTINUE_STRAIGHT => "|"
      case ExtraData.VALUE_RTE_ACTION_NO_MANEUVER_NAME_CHANGE => ""
      case ExtraData.VALUE_RTE_ACTION_LEFT_SLIGHT => "<"
      case ExtraData.VALUE_RTE_ACTION_LEFT => "<="
      case ExtraData.VALUE_RTE_ACTION_LEFT_SHARP => "<=="
      case ExtraData.VALUE_RTE_ACTION_RIGHT_SLIGHT => ">"
      case ExtraData.VALUE_RTE_ACTION_RIGHT => "=>"
      case ExtraData.VALUE_RTE_ACTION_RIGHT_SHARP => "==>"
      case ExtraData.VALUE_RTE_ACTION_STAY_LEFT => "<= S"
      case ExtraData.VALUE_RTE_ACTION_STAY_RIGHT => "S =>"
      case ExtraData.VALUE_RTE_ACTION_STAY_STRAIGHT => "S |"
      case ExtraData.VALUE_RTE_ACTION_U_TURN => "U"
      case ExtraData.VALUE_RTE_ACTION_U_TURN_LEFT => "<= U"
      case ExtraData.VALUE_RTE_ACTION_U_TURN_RIGHT => "U =>"
      case ExtraData.VALUE_RTE_ACTION_EXIT_LEFT => "<= E"
      case ExtraData.VALUE_RTE_ACTION_EXIT_RIGHT => "E =>"
      case ExtraData.VALUE_RTE_ACTION_RAMP_ON_LEFT => "<= R"
      case ExtraData.VALUE_RTE_ACTION_RAMP_ON_RIGHT => "R =>"
      case ExtraData.VALUE_RTE_ACTION_RAMP_STRAIGHT => "R |"
      case ExtraData.VALUE_RTE_ACTION_MERGE_LEFT => "<= M"
      case ExtraData.VALUE_RTE_ACTION_MERGE_RIGHT => "M =>"
      case ExtraData.VALUE_RTE_ACTION_MERGE => "M"
      case ExtraData.VALUE_RTE_ACTION_ENTER_STATE => ""
      case ExtraData.VALUE_RTE_ACTION_ARRIVE_DEST => "."
      case ExtraData.VALUE_RTE_ACTION_ARRIVE_DEST_LEFT => "<= ."
      case ExtraData.VALUE_RTE_ACTION_ARRIVE_DEST_RIGHT => ". =>"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_1 => "(1)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_2 => "(2)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_3 => "(3)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_4 => "(4)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_5 => "(5)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_6 => "(6)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_7 => "(7)"
      case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_8 => "(8)"
      case ExtraData.VALUE_RTE_ACTION_PASS_PLACE => "POI"
    })
  }

  private var lastUpdate: Option[UpdateContainer] = None

  def toggleRecording() {
    lastUpdate match {
      case Some(u) if u.isTrackRecRecording()
                   && !u.getTrackRecordContainer().isTrackRecPaused() =>
        ActionTools.actionTrackRecordPause(ctx, locusVer)
      case _ =>
        ActionTools.actionTrackRecordStart(ctx, locusVer)
    }
  }
}
