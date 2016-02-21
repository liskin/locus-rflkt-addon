/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.{BroadcastReceiver, Context, Intent}

class PeriodicUpdateReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent) {
    info(s"PeriodicUpdateReceiver.onReceive called")
  }

  private[this] implicit lazy val loggerTag = LoggerTag("LocusRflktAddon")
}
