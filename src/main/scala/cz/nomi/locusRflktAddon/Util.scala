/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import scala.reflect.Manifest
import scala.concurrent.{ExecutionContext, Future}

import android.app.{Service, Activity, Fragment}
import android.os.{Binder, IBinder, Bundle, AsyncTask, Handler}
import android.content.{Context, Intent, ServiceConnection, ComponentName,
  IntentFilter, BroadcastReceiver, SharedPreferences}
import android.preference.PreferenceManager
import android.view.{Menu, MenuItem}
import android.support.v7.app.AppCompatActivity
import android.support.v4.app.NavUtils

import macroid.{Contexts, ContextWrapper, IdGeneration}

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
  private var onServiceConnectedBodies: List[() => Unit] = Nil

  def onServiceConnected(body: => Unit) {
    onServiceConnectedBodies ::= body _
  }

  override def onServiceConnected(cn: ComponentName, b: IBinder) {
    service = Some(b.asInstanceOf[S#LocalServiceBinder].service)
    onServiceConnectedBodies.reverse.foreach(_())
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
  import android.support.v4.content.LocalBroadcastManager

  implicit def strToIntentFilter(str: String): IntentFilter =
    new IntentFilter(str)

  def broadcastReceiver(filter: IntentFilter,
      broadcastPermission: String = null)
    (onReceiveBody: (Context, Intent) => Unit)
    (implicit ctx: Context, reg: Registerable)
  {
    val receiver = new BroadcastReceiver {
      def onReceive(context: Context, intent: Intent) {
        onReceiveBody(context, intent)
      }
    }
    reg.onRegister(ctx.registerReceiver(receiver, filter, broadcastPermission, null))
    reg.onUnregister(ctx.unregisterReceiver(receiver))
  }

  def localBroadcastReceiver(filter: IntentFilter)
    (onReceiveBody: (Context, Intent) => Unit)
    (implicit ctx: Context, reg: Registerable)
  {
    val receiver = new BroadcastReceiver {
      def onReceive(context: Context, intent: Intent) {
        onReceiveBody(context, intent)
      }
    }
    def localManager = LocalBroadcastManager.getInstance(ctx)
    reg.onRegister(localManager.registerReceiver(receiver, filter))
    reg.onUnregister(localManager.unregisterReceiver(receiver))
  }

  def sendLocalBroadcast(intent: Intent)(implicit ctx: Context) {
    val localManager = LocalBroadcastManager.getInstance(ctx)
    localManager.sendBroadcast(intent)
  }
}

// inspired by scaloid
abstract class PreferenceVar[T](key: String, defaultValue: T) {
  protected def get(value: T, pref: SharedPreferences): T
  protected def put(value: T, editor: SharedPreferences.Editor): Unit

  final def apply(pref: SharedPreferences): T =
    get(defaultValue, pref)

  final def update(pref: SharedPreferences, value: T) {
    val editor = pref.edit()
    put(value, editor)
    editor.apply()
  }

  final def remove(pref: SharedPreferences) {
    pref.edit().remove(key).apply()
  }
}

// inspired by scaloid
object Preferences {
  def defaultSharedPreferences(implicit context: Context): SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)

  def preferenceVar[T](key: String, defaultVal: T): PreferenceVar[T] =
    defaultVal match {
      case v: String =>
        new PreferenceVar[String](key, v) {
          def get(value: String, pref: SharedPreferences): String =
            pref.getString(key, value)
          def put(value: String, editor: SharedPreferences.Editor): Unit =
            editor.putString(key, value)
        }.asInstanceOf[PreferenceVar[T]]
      case v: Boolean =>
        new PreferenceVar[Boolean](key, v) {
          def get(value: Boolean, pref: SharedPreferences): Boolean =
            pref.getBoolean(key, value)
          def put(value: Boolean, editor: SharedPreferences.Editor): Unit =
            editor.putBoolean(key, value)
        }.asInstanceOf[PreferenceVar[T]]
    }
}

// inspired by scaloid
trait Registerable {
  protected implicit val implicitRegisterable: Registerable = this

  def onRegister(body: => Unit): Unit
  def onUnregister(body: => Unit): Unit
}

// inspired by scaloid
trait OnCreateDestroy {
  protected var onCreateBodies: List[() => Unit] = Nil
  protected var onDestroyBodies: List[() => Unit] = Nil

  def onCreate(body: => Unit) {
    onCreateBodies ::= body _
  }

  def onDestroy(body: => Unit) {
    onDestroyBodies ::= body _
  }
}

// inspired by scaloid
trait OnResumePause {
  protected var onResumeBodies: List[() => Unit] = Nil
  protected var onPauseBodies: List[() => Unit] = Nil

  def onResume(body: => Unit) {
    onResumeBodies ::= body _
  }

  def onPause(body: => Unit) {
    onPauseBodies ::= body _
  }
}

trait Notice {
  def notice(s: String)(implicit ctx: ContextWrapper) {
    import macroid.FullDsl._
    (toast(s) <~ fry).run
  }
}

trait OptionsMenu {
  protected var onCreateOptionsMenuBodies: List[Menu => Unit] = Nil
  protected var onPrepareOptionsMenuBodies: List[Menu => Unit] = Nil

  def onCreateOptionsMenu(body: Menu => Unit) {
    onCreateOptionsMenuBodies ::= body
  }

  def onPrepareOptionsMenu(body: Menu => Unit) {
    onPrepareOptionsMenuBodies ::= body
  }

  def onMenuClick(mi: MenuItem)(f: MenuItem => Unit): MenuItem =
    mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener {
      def onMenuItemClick(mi: MenuItem): Boolean = { f(mi); true }
    })
}

// inspired by scaloid
trait RService extends Service with Contexts[Service]
  with OnCreateDestroy with Registerable with Notice
{
  protected implicit val implicitContext: Context = this

  override def onCreate() {
    super.onCreate()
    onCreateBodies.reverse.foreach(_())
  }

  override def onDestroy() {
    onDestroyBodies.foreach(_())
    super.onDestroy()
  }

  def onRegister(body: => Unit): Unit = onCreate(body)
  def onUnregister(body: => Unit): Unit = onDestroy(body)
}

// inspired by scaloid
trait RActivity extends Activity with Contexts[Activity]
  with OnCreateDestroy with OnResumePause with Registerable with OptionsMenu
{
  protected implicit val implicitContext: Context = this

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    onCreateBodies.reverse.foreach(_())
  }

  override def onDestroy() {
    onDestroyBodies.foreach(_())
    super.onDestroy()
  }

  override def onResume() {
    super.onResume()
    onResumeBodies.reverse.foreach(_())
  }

  override def onPause() {
    onPauseBodies.foreach(_())
    super.onPause()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    onCreateOptionsMenuBodies.reverse.foreach(_(menu))
    super.onCreateOptionsMenu(menu)
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    onPrepareOptionsMenuBodies.reverse.foreach(_(menu))
    super.onPrepareOptionsMenu(menu)
  }

  def onRegister(body: => Unit): Unit = onResume(body)
  def onUnregister(body: => Unit): Unit = onPause(body)
}

trait RFragment extends Fragment with Contexts[Fragment]
  with OnCreateDestroy with OnResumePause with Registerable
{
  override def onCreate(b: Bundle) {
    super.onCreate(b)
    onCreateBodies.reverse.foreach(_())
  }

  override def onDestroy() {
    onDestroyBodies.foreach(_())
    super.onDestroy()
  }

  override def onResume() {
    super.onResume()
    onResumeBodies.reverse.foreach(_())
  }

  override def onPause() {
    onPauseBodies.foreach(_())
    super.onPause()
  }

  def onRegister(body: => Unit): Unit = onResume(body)
  def onUnregister(body: => Unit): Unit = onPause(body)
}

trait BackToParentActivity extends RActivity { this: AppCompatActivity =>
  onCreate {
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        NavUtils.navigateUpFromSameTask(this)
        true
      case _ =>
        super.onOptionsItemSelected(item)
    }
  }
}

object Gen extends IdGeneration

object Async {
  object Implicits {
    implicit val ecThreadPool =
      ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    implicit val ecSerial =
      ExecutionContext.fromExecutor(AsyncTask.SERIAL_EXECUTOR)
  }

  def apply[T](async: => T)(sync: T => Unit)(implicit ec: ExecutionContext) {
    val handler = new Handler()
    Future {
      val t = async
      handler.post(new Runnable { def run() = sync(t) })
    }
  }
}
