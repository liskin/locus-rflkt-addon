package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.{BroadcastReceiver, Context, Intent}
import locus.api.android.features.periodicUpdates.{PeriodicUpdatesHandler, UpdateContainer}
import locus.api.android.utils.LocusUtils.LocusVersion

class PeriodicUpdateReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent) {
    info(s"PeriodicUpdateReceiver.onReceive called")

    if (intent == null || intent.getAction == null)
      return

    PeriodicUpdatesHandler.getInstance.onReceive(context, intent, OnUpdate)
  }

  object OnUpdate extends PeriodicUpdatesHandler.OnUpdate {
    def onIncorrectData() {
      // TODO: log something
    }

    def onUpdate(version: LocusVersion, update: UpdateContainer) {
      info(s"PUR: getGpsSatsAll: ${update.getGpsSatsAll}")
    }
  }

  private[this] implicit val loggerTag = LoggerTag("LocusRflktAddon")
}
