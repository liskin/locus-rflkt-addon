/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{BroadcastReceiver, Context, Intent}

import Log._

class PeriodicUpdateReceiver extends BroadcastReceiver {

  override def onReceive(context: Context, intent: Intent) {
    logger.info(s"PeriodicUpdateReceiver.onReceive called")
  }
}
