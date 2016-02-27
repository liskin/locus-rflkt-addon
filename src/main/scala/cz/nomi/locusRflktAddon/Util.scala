/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import org.scaloid.common._

import scala.reflect.Manifest

import android.app.Service
import android.os.{Binder, IBinder}
import android.content.{Context, Intent, ServiceConnection, ComponentName}

trait Log extends TagUtil {
  override implicit val loggerTag = LoggerTag("LocusRflktAddon")
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
