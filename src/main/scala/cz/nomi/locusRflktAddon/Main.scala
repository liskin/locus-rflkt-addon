package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.{Context, Intent, IntentFilter}

import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.LocusConst
import locus.api.android.utils.LocusUtils
import locus.api.android.ActionTools

class Main extends SActivity with Log {
  val hwCon = new LocalServiceConnection[HardwareConnectorService]

  lazy val meToo = new STextView("Me too")

  onCreate {
    info(s"create")

    contentView = new SVerticalLayout {
      meToo.here
      SButton(R.string.red).onClick(meToo.text = "pressed")
      SButton("enable discovery").onClick {
        hwCon(_.enableDiscovery(true))
      }
      SButton("disable discovery").onClick {
        hwCon(_.enableDiscovery(false))
      }
      SButton("connect first").onClick {
        hwCon(_.connectFirst())
      }
    }

    refreshPeriodicUpdateListeners()
  }

  broadcastReceiver(LocusConst.ACTION_PERIODIC_UPDATE: IntentFilter) { (context: Context, intent: Intent) =>
    info(s"periodic update received")
    PeriodicUpdatesHandler.getInstance.onReceive(context, intent, OnUpdate)
  }

  private def refreshPeriodicUpdateListeners() {
    info("refreshPeriodicUpdateListeners")
    val ver = LocusUtils.getActiveVersion(ctx)
    val locusInfo = ActionTools.getLocusInfo(ctx, ver)
    info(s"periodic updates: ${locusInfo.isPeriodicUpdatesEnabled}")
    ActionTools.refreshPeriodicUpdateListeners(ctx, ver)
  }

  private object OnUpdate extends PeriodicUpdatesHandler.OnUpdate {
    import LocusUtils.LocusVersion

    def onIncorrectData() {
      // TODO: log something
    }

    def onUpdate(version: LocusVersion, update: UpdateContainer) {
      info(s"getGpsSatsAll: ${update.getGpsSatsAll}")
    }
  }
}
