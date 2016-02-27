/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import scala.reflect.Manifest

import android.app.Service
import android.os.{Binder, IBinder}
import android.content.{Context, Intent, ServiceConnection, ComponentName, IntentFilter, BroadcastReceiver}

// inspired by scaloid
object Log {
  import android.util.{Log => L}

  private val tag = "LocusRflktAddon"

  private def loggingText(str: String, t: Throwable) = str + (if (t == null) "" else "\n" + L.getStackTraceString(t))

  def verbose(str: => String, t: Throwable = null): Unit =
    if (L.isLoggable(tag, L.VERBOSE))
      L.v(tag, loggingText(str, t))

  def debug(str: => String, t: Throwable = null): Unit =
    if (L.isLoggable(tag, L.DEBUG))
      L.d(tag, loggingText(str, t))

  def info(str: => String, t: Throwable = null): Unit =
    if (L.isLoggable(tag, L.INFO))
      L.i(tag, loggingText(str, t))

  def warn(str: => String, t: Throwable = null): Unit =
    if (L.isLoggable(tag, L.WARN))
      L.w(tag, loggingText(str, t))

  def error(str: => String, t: Throwable = null): Unit =
    if (L.isLoggable(tag, L.ERROR))
      L.e(tag, loggingText(str, t))

  def wtf(str: => String, t: Throwable = null): Unit =
    if (L.isLoggable(tag, L.ASSERT))
      L.wtf(tag, loggingText(str, t))
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
