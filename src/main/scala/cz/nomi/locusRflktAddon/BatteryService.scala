/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{Context, Intent}
import android.os.BatteryManager

import Broadcasts._

trait BatteryService extends RService with RflktApi {
  broadcastReceiver(Intent.ACTION_BATTERY_CHANGED)
  { (context: Context, intent: Intent) =>
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level != -1 && scale != -1) {
      import display.Const.{Widget => W}
      import Formatters.formatInt

      setRflkt(s"${W.battery}.value" -> formatInt(Some(100 * level / scale)))
    }
  }
}
