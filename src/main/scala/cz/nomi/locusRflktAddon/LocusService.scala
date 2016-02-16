package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.{Context, Intent, IntentFilter}

import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.{LocusConst, LocusUtils}
import locus.api.android.ActionTools
import LocusUtils.LocusVersion

trait LocusApi {
  // nothing yet
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

  private def locusVer: LocusVersion = {
    //val locusInfo = ActionTools.getLocusInfo(ctx, ver)
    //info(s"periodic updates: ${locusInfo.isPeriodicUpdatesEnabled}")
    LocusUtils.getActiveVersion(ctx)
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
      val now = java.util.Calendar.getInstance().getTime()

      val loc = update.getLocMyLocation()
      val curSpeed = Option(loc.getSpeed()).filter(_ != 0).map(_ * 36 / 10)
      val curHeartRate = Option(loc.getSensorHeartRate()).filter(_ != 0)
      val curCadence = Option(loc.getSensorCadence()).filter(_ != 0)

      val trackRecord = Option(update.getTrackRecordContainer())
      val avgSpeed = trackRecord.map(_.getSpeedAvg()).filter(_ != 0).map(_ * 36 / 10)
      val distance = trackRecord.map(_.getDistance() / 1000)

      setRflkt(
        "CLOCK.value" -> timeFormat.format(now),
        "SPEED_CURRENT.value" -> formatFloat(curSpeed),
        "SPEED_WORKOUT_AV.value" -> formatFloat(avgSpeed),
        "DISTANCE_WORKOUT.value" -> formatDouble(distance),
        "BIKE_CAD_CURRENT.value" -> formatInt(curCadence),
        "HR_CURRENT.value" -> formatInt(curHeartRate)
      )
    }

    private val timeFormat = new java.text.SimpleDateFormat("HH:mm:ss")

    private def formatInt(f: Option[Int]): String =
      f.map(v => f"$v%d").getOrElse("--")

    private def formatFloat(f: Option[Float]): String =
      f.map(v => f"$v%.1f").getOrElse("--")

    private def formatDouble(f: Option[Double]): String =
      f.map(v => f"$v%.1f").getOrElse("--")
  }
}
