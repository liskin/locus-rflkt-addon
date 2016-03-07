/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import com.github.ghik.silencer.silent

import android.content.{Context, Intent}
import android.provider.Telephony.Sms.{Intents => SmsIntents}
import android.telephony.SmsMessage
import android.Manifest.permission
import android.os.Build.{VERSION_CODES => VersionCodes, VERSION => Version}

import Log._
import Broadcasts._

trait NotificationService extends RService
{ this: RflktApi =>
  import NotificationService._

  broadcastReceiver(SmsIntents.SMS_RECEIVED_ACTION,
    broadcastPermission = permission.BROADCAST_SMS)
  { (context: Context, intent: Intent) =>
    val msgs = getMessagesFromIntent(intent).filter(_ != null)
    msgs.headOption.foreach(receivedSms)
  }

  private def receivedSms(msg: SmsMessage) {
    import display.Const.{Widget => W}
    import display.Const.{Page => P}
    import RflktApi.Str
    import Formatters.normalizeString

    val addr = msg.getDisplayOriginatingAddress()
    val body = msg.getDisplayMessageBody()
    logger.info(s"""NotificationService: SMS from "$addr": "$body"""")

    // TODO: wrap at word boundaries
    val bodyLines = body.grouped(14).toArray
    def bodyLine(i: Int) =
      normalizeString(bodyLines.applyOrElse(i, (_: Int) => ""))

    setRflkt(
      // TODO: show name instead of number
      s"${W.notifHeader}.value" -> Str(addr),
      s"${W.notifLine(0)}.value" -> Str(bodyLine(0)),
      s"${W.notifLine(1)}.value" -> Str(bodyLine(1)),
      s"${W.notifLine(2)}.value" -> Str(bodyLine(2)),
      s"${W.notifLine(3)}.value" -> Str(bodyLine(3)),
      s"${W.notifLine(4)}.value" -> Str(bodyLine(4)),
      s"${W.notifLine(5)}.value" -> Str(bodyLine(5)),
      s"${W.notifLine(6)}.value" -> Str(bodyLine(6))
    )

    setRflktPage(P.notification)
  }
}

object NotificationService {
  private def getMessagesFromIntent(intent: Intent): Array[SmsMessage] = {
    if (Version.SDK_INT >= VersionCodes.KITKAT) {
      SmsIntents.getMessagesFromIntent(intent)
    } else {
      intent.getExtras.get("pdus").asInstanceOf[Array[Array[Byte]]]
        .map(SmsMessage.createFromPdu: @silent)
    }
  }
}
