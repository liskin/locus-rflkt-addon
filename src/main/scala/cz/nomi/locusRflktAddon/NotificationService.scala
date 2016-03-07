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
    val addr = msg.getDisplayOriginatingAddress()
    val body = msg.getDisplayMessageBody()

    import Async.Implicits.ecSerial
    Async(findContactName(addr)) { name =>
      setNotification(name, body)
      setRflktPage(display.Const.Page.notification)
    }
  }

  private def setNotification(header: String, body: String) {
    import display.Const.{Widget => W}
    import RflktApi.Str
    import Formatters.normalizeString

    val bodyLines = breakIntoLines(normalizeString(body)).toArray
    def bodyLine(i: Int) = bodyLines.applyOrElse(i, (_: Int) => "")

    setRflkt(
      s"${W.notifHeader}.value" -> Str(normalizeString(header)),
      s"${W.notifLine(0)}.value" -> Str(bodyLine(0)),
      s"${W.notifLine(1)}.value" -> Str(bodyLine(1)),
      s"${W.notifLine(2)}.value" -> Str(bodyLine(2)),
      s"${W.notifLine(3)}.value" -> Str(bodyLine(3)),
      s"${W.notifLine(4)}.value" -> Str(bodyLine(4)),
      s"${W.notifLine(5)}.value" -> Str(bodyLine(5)),
      s"${W.notifLine(6)}.value" -> Str(bodyLine(6))
    )
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

  private def breakIntoLines(str: String, maxLine: Int = 14): List[String] = {
    import java.text.BreakIterator

    val iter = BreakIterator.getLineInstance()
    iter.setText(str)

    def makeLine(words: List[String]): List[String] =
      if (words.nonEmpty) List(words.reverse.mkString(" ")) else Nil

    def lines(len: Int, words: List[String]): List[String] = {
      assert(len <= maxLine)
      val (start, end) = (iter.current(), iter.next())
      if (end == BreakIterator.DONE) {
        makeLine(words)
      } else {
        val word = str.substring(start, end).trim
        if (word.length == 0) {
          lines(len, words)
        } else if (len + 1 + word.length <= maxLine) {
          lines(len + 1 + word.length, word :: words)
        } else {
          makeLine(words) ++ newLine(word)
        }
      }
    }

    def newLine(word: String): List[String] =
      if (word.length > maxLine) {
        val (a, b) = word.splitAt(maxLine)
        a :: newLine(b)
      } else {
        lines(word.length, List(word))
      }

    lines(0, Nil)
  }

  private def findContactName(number: String)(implicit ctx: Context): String = {
    import android.database.Cursor
    import android.provider.ContactsContract.{PhoneLookup, ContactsColumns}
    import android.net.Uri

    var cursor: Cursor = null
    try {
      val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
      val proj = Array(ContactsColumns.DISPLAY_NAME)
      cursor = ctx.getContentResolver().query(uri, proj, null, null, null)

      if (cursor != null && cursor.moveToFirst()) {
        cursor.getString(cursor.getColumnIndex(proj(0)))
      } else {
        number
      }
    } catch {
      case e: Exception =>
        logger.error(e)("findContactName exception")
        number
    } finally {
      if (cursor != null) cursor.close()
    }
  }
}
