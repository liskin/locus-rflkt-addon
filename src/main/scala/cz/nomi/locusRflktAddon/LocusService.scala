package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.{Context, Intent, IntentFilter}

import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.{LocusConst, LocusUtils}
import locus.api.android.ActionTools
import LocusUtils.LocusVersion

class LocusService extends LocalService with Log {
  val hwCon = new LocalServiceConnection[HardwareConnectorService]

  onCreate {
    info(s"LocusService: onCreate")
  }

  onDestroy {
    info(s"LocusService: onDestroy")
  }

  override def onTaskRemoved(rootIntent: Intent) {
    stopSelf()
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
      val trackRecord = Option(update.getTrackRecordContainer())
      val vars = Map(
        "CLOCK.value" -> timeFormat.format(now),
        "SPEED_CURRENT.value" -> formatFloat(trackRecord.map(_.getSpeedAvg() * 36 / 10)), // FIXME: current, not avg
        "DISTANCE_WORKOUT.value" -> formatDouble(trackRecord.map(_.getDistance() / 1000)),
        "BIKE_CAD_CURRENT.value" -> "--",
        "HR_CURRENT.value" -> "--"
      )
      hwCon(_.setRflkt(vars))
    }

    private val timeFormat = new java.text.SimpleDateFormat("HH:mm:ss")

    private def formatFloat(f: Option[Float]): String =
      f.map(v => f"$v%.1f").getOrElse("--")

    private def formatDouble(f: Option[Double]): String =
      f.map(v => f"$v%.1f").getOrElse("--")
  }
}
