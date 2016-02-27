/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import scala.reflect.Manifest

import android.app.Service
import android.os.{Binder, IBinder}
import android.content.{Context, Intent, ServiceConnection, ComponentName,
  IntentFilter, BroadcastReceiver, SharedPreferences}
import android.preference.PreferenceManager

object Log {
  val logger = org.log4s.getLogger("LocusRflktAddon")
}

// inspired by scaloid
trait LocalService[+T] extends Service { localService: T =>
  private val binder = new LocalServiceBinder

  override def onBind(intent: Intent): IBinder = binder

  class LocalServiceBinder extends Binder {
    def service: T = localService
  }
}

// inspired by scaloid
final class LocalServiceConnection[S <: LocalService[S]]
  (bindFlag: Int = Context.BIND_AUTO_CREATE)
  (implicit ctx: Context, reg: Registerable, manifest: Manifest[S])
  extends ServiceConnection
{
  private var service: Option[S] = None

  override def onServiceConnected(cn: ComponentName, b: IBinder) {
    service = Some(b.asInstanceOf[S#LocalServiceBinder].service)
  }

  override def onServiceDisconnected(cn: ComponentName) {
    service = None
  }

  def apply[T](f: S => T): Option[T] = service.map(f)

  reg.onRegister {
    val intent = new Intent(ctx, manifest.runtimeClass)
    ctx.bindService(intent, this, bindFlag)
  }

  reg.onUnregister {
    if (service.isDefined) {
      service = None
      ctx.unbindService(this)
    }
  }
}

// inspired by scaloid
object Broadcasts {
  import scala.language.implicitConversions

  implicit def strToIntentFilter(str: String): IntentFilter =
    new IntentFilter(str)

  def broadcastReceiver(filter: IntentFilter)
    (onReceiveBody: (Context, Intent) => Unit)
    (implicit ctx: Context, reg: Registerable)
  {
    val receiver = new BroadcastReceiver {
      def onReceive(context: Context, intent: Intent) {
        onReceiveBody(context, intent)
      }
    }
    reg.onRegister(ctx.registerReceiver(receiver, filter))
    reg.onUnregister(ctx.unregisterReceiver(receiver))
  }

  // TODO: local broadcasts
}

// inspired by scaloid
abstract class PreferenceVar[T](key: String, defaultValue: T) {
  protected def get(value: T, pref: SharedPreferences): T
  protected def put(value: T, editor: SharedPreferences.Editor): Unit

  final def apply()(implicit pref: SharedPreferences): T =
    get(defaultValue, pref)

  final def update(value: T)(implicit pref: SharedPreferences) {
    val editor = pref.edit()
    put(value, editor)
    editor.apply()
  }

  final def remove()(implicit pref: SharedPreferences) {
    pref.edit().remove(key).apply()
  }
}

// inspired by scaloid
object Preferences {
  implicit def defaultSharedPreferences(implicit context: Context): SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)

  def preferenceVar[T](key: String, defaultVal: T): PreferenceVar[T] =
    defaultVal match {
      case v: String => new PreferenceVar[String](key, v) {
        def get(value: String, pref: SharedPreferences): String =
          pref.getString(key, value)
        def put(value: String, editor: SharedPreferences.Editor): Unit =
          editor.putString(key, value)
      }.asInstanceOf[PreferenceVar[T]]
    }
}
