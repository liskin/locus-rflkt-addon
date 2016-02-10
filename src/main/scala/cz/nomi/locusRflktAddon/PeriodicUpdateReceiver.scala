package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.{BroadcastReceiver, Context, Intent}

class PeriodicUpdateReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent) {
    info(s"PeriodicUpdateReceiver.onReceive called")
  }

  private[this] implicit lazy val loggerTag = LoggerTag("LocusRflktAddon")
}
