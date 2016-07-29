/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import scala.collection.JavaConversions._

import android.app.{NotificationManager, PendingIntent, Service}
import android.content.{Intent, Context}
import android.support.v4.app.NotificationCompat
import android.bluetooth.BluetoothAdapter
import android.os.CountDownTimer

import java.util.UUID

import com.wahoofitness.connector
import connector.HardwareConnector
import connector.HardwareConnectorTypes.{NetworkType, SensorType}
import connector.HardwareConnectorEnums.HardwareConnectorState
import connector.conn.connections
import connections.SensorConnection
import connections.params.{ConnectionParams, BTLEConnectionParams}
import connector.listeners.discovery.DiscoveryListener
import connector.capabilities
import capabilities.Capability.CapabilityType
import capabilities.ConfirmConnection
import capabilities.Rflkt
import Rflkt.{ButtonPressType, LoadConfigResult}
import connector.HardwareConnectorEnums.{SensorConnectionError, SensorConnectionState}
import com.wahoofitness.common.display.{DisplayConfiguration, DisplayButtonPosition}

import Log._
import Broadcasts._
import Preferences._
import Const._

trait RflktApi {
  def setRflkt(vars: (String, RflktApi.Val)*): Unit
  def setRflktPage(page: String, timeout: Option[Int] = None): Unit
  def onButtonPressed(fun: String, typ: ButtonPressType): Unit
  def onLoadComplete(conf: DisplayConfiguration): Unit
}

object RflktApi {
  sealed abstract class Val
  case class Str(s: String) extends Val
  case class Vis(v: Boolean) extends Val
}

trait RflktService extends ForegroundService with RflktApi {
  private var hwCon: HardwareConnector = null
  private var curSensor: Option[SensorConnection] = None
  private var status: String = ""
  private var availableElements: Set[String] = Set()
  private val rflktVars: collection.mutable.Map[String, RflktApi.Val] = collection.mutable.Map.empty

  onRegister {
    hwCon = new HardwareConnector(this, Hardware)
  }

  onUnregister {
    hwCon.stopDiscovery()
    hwCon.shutdown()
  }

  private object Hardware extends HardwareConnector.Listener {
    override def connectedSensor(s: SensorConnection) {
      logger.info(s"connectedSensor: $s")
    }

    override def disconnectedSensor(s: SensorConnection) {
      logger.info(s"disconnectedSensor: $s")
    }

    override def connectorStateChanged(nt: NetworkType, state: HardwareConnectorState) {
      logger.info(s"connectorStateChanged: $nt, $state")
    }

    override def onFirmwareUpdateRequired(s: SensorConnection, current: String, recommended: String) {
      logger.info(s"onFirmwareUpdateRequired: $s, $current, $recommended")
    }
  }

  private object Discovery extends DiscoveryListener {
    override def onDeviceDiscovered(params: ConnectionParams) {
      if (!params.hasCapability(CapabilityType.Rflkt))
        return

      logger.info(s"onDeviceDiscovered: $params")
      notice(s"discovered: ${params.getName}")
      refreshUi()
    }

    override def onDiscoveredDeviceLost(params: ConnectionParams) {
      if (!params.hasCapability(CapabilityType.Rflkt))
        return

      logger.info(s"onDiscoveredDeviceLost: $params")
      notice(s"lost: ${params.getName}")
      refreshUi()
    }

    override def onDiscoveredDeviceRssiChanged(params: ConnectionParams, rssi: Int) {
      logger.info(s"onDiscoveredDeviceRssiChanged: $params, $rssi")
    }
  }

  private object Connection extends SensorConnection.Listener {
    override def onNewCapabilityDetected(s: SensorConnection, typ: CapabilityType) {
      logger.info(s"onNewCapabilityDetected: $s, $typ")

      typ match {
        case CapabilityType.ConfirmConnection =>
          getCapConfirm().get.addListener(Confirmation)
          requestConfirmation()
          refreshUi("waiting for confirmation")
        case CapabilityType.Rflkt =>
          getCapRflkt().get.addListener(RFLKT)
        case _ =>
      }
    }

    override def onSensorConnectionError(s: SensorConnection, e: SensorConnectionError) {
      logger.info(s"onSensorConnectionError: $s, $e")
      notice(s"${s.getDeviceName}: $e")
    }

    override def onSensorConnectionStateChanged(s: SensorConnection, state: SensorConnectionState) {
      logger.info(s"onSensorConnectionStateChanged: $s, $state")

      if (state == SensorConnectionState.DISCONNECTED) {
        curSensor = None
      } else {
        curSensor = Some(s)
        if (state == SensorConnectionState.CONNECTED) {
          lastSensor(defaultSharedPreferences) =
            s.getConnectionParams.serialize
          getCapConfirm().foreach { confirm =>
            val state = confirm.getState()
            if (state != ConfirmConnection.State.ACCEPTED && !state.isBusy())
              requestConfirmation()
          }
        }
      }

      notice(s"${s.getDeviceName}: $state")
      updateNotification(s"$state")
      refreshUi(s"$state")
    }
  }

  private object Confirmation extends ConfirmConnection.Listener {
    override def onConfirmationProcedureStateChange(state: ConfirmConnection.State, error: ConfirmConnection.Error) {
      logger.info(s"onConfirmationProcedureStateChange: $state, $error")
      if (state == ConfirmConnection.State.FAILED) {
        requestConfirmation()
        refreshUi("still waiting for confirmation")
      }
    }

    override def onUserAccept() {
      logger.info(s"onUserAccept")
      getCapRflkt().foreach { rflkt =>
        rflkt.sendSetBacklightPercent(0)
        if (rflkt.getLastLoadConfigResult() != LoadConfigResult.SUCCESS) {
          loadConfig()
          refreshUi("loading config")
        }
      }
    }
  }

  private object RFLKT extends Rflkt.Listener {
    import connector.packets.dcp.response.DCPR_DateDisplayOptionsPacket._

    override def onAutoPageScrollRecieved() {}
    override def onBacklightPercentReceived(p: Int) {}
    override def onButtonPressed(pos: DisplayButtonPosition, typ: ButtonPressType) {
      getCapRflkt() foreach { rflkt =>
        val buttonCfg = Option(rflkt.getDisplayConfiguration()).map(_.getButtonCfg())
        val buttonCfgPage = Option(rflkt.getPage()).map(_.getButtonCfg())
        val fun =
          buttonCfgPage.flatMap(c => Option(c.getButtonFunction(pos))) orElse
          buttonCfg.flatMap(c => Option(c.getButtonFunction(pos))) getOrElse null
        logger.info(s"onButtonPressed: $pos, $fun, $typ")
        RflktService.this.onButtonPressed(fun, typ)
      }
    }
    override def onColorInvertedReceived(inverted: Boolean) {}
    override def onConfigVersionsReceived(ver: Array[Int]) {}
    override def onDateReceived(date: java.util.Calendar) {}
    override def onDisplayOptionsReceived(x1: DisplayDateFormat, x2: DisplayTimeFormat, x3: DisplayDayOfWeek, x4: DisplayWatchFaceStyle) {}
    override def onLoadComplete() {
      logger.info(s"onLoadComplete")
      RflktService.this.onLoadComplete(getCapRflktReady().get.getDisplayConfiguration())
      refreshUi("everything okay")
    }
    override def onLoadFailed(result: LoadConfigResult) {
      logger.info(s"onLoadFailed: $result")
      refreshUi("load failed :-(")
    }
    override def onLoadProgressChanged(progress: Int) {
      logger.info(s"onLoadProgressChanged: $progress")
      refreshUi(s"loading config... $progress/100")
    }
    override def onPageIndexReceived(index: Int) {}
  }

  def enableDiscovery(enable: Boolean) {
    enableBluetooth()
    enable match {
      case true =>
        logger.info(s"startDiscovery")
        hwCon.startDiscovery(Discovery)
      case false =>
        logger.info(s"stopDiscovery")
        hwCon.stopDiscovery()
    }
  }

  def isDiscovering(): Boolean = hwCon.isDiscovering()

  def isDisconnected(): Boolean = curSensor.isEmpty

  def getStatus(): String = status

  private def getFirst(): Option[ConnectionParams] = {
    val rflkts = hwCon.getDiscoveredConnectionParams()
      .filter(_.hasCapability(CapabilityType.Rflkt))
    rflkts.headOption orElse lastSensorOption
  }

  def describeFirst(): Option[String] =
    getFirst().map { p =>
      val n = p.getName()
      p match {
        case b: BTLEConnectionParams =>
          val a = b.getBluetoothDevice().getAddress()
          s"$n ($a)"
        case _ =>
          n
      }
    }

  def connectFirst() {
    getFirst().foreach { p =>
      startForeground()
      enableBluetooth()
      hwCon.requestSensorConnection(p, Connection)
      hwCon.stopDiscovery()
    }
  }

  def disconnect() {
    curSensor.foreach(_.disconnect())
    stopForeground()
  }

  def stopUnneeded() {
    stopUnlessForeground()
  }

  private lazy val refreshUiIntent = new Intent(localActionRefreshUi)

  private def refreshUi(newStatus: String = status) {
    status = newStatus
    sendLocalBroadcast(refreshUiIntent)
  }

  private def enableBluetooth() {
    if (hwCon.getHardwareConnectorState(NetworkType.BTLE) == HardwareConnectorState.HARDWARE_NOT_ENABLED) {
      val intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      startActivity(intent)
    }
  }

  private def getCap[T](typ: CapabilityType): Option[T] =
    curSensor.filter(_.isConnected())
      .map(_.getCurrentCapability(typ).asInstanceOf[T]).filter(_ != null)

  private def getCapConfirm(): Option[ConfirmConnection] =
    getCap(CapabilityType.ConfirmConnection)

  private def getCapRflkt(): Option[Rflkt] =
    getCap(CapabilityType.Rflkt)

  private def getCapRflktReady(): Option[Rflkt] =
    getCapRflkt().filter(_.getLastLoadConfigResult() == LoadConfigResult.SUCCESS)

  private def requestConfirmation() {
    getCapConfirm() foreach {
      _.requestConfirmation(ConfirmConnection.Role.MASTER, "Locus", getUuid, "LocusRflktAddon")
    }
  }

  private def loadConfig() =
    getCapRflkt() foreach { rflkt =>
      val sp = defaultSharedPreferences
      val conf = display.Pages.conf(
        ButtonSettings.getValue(sp),
        PageSettings.getValue(sp)
      )
      rflkt.loadConfig(conf)
    }

  def setRflkt(vars: (String, RflktApi.Val)*) {
    rflktVars ++= vars
    getCapRflktReady().foreach(doSetRflkt(_, vars: _*))
  }

  private def doSetRflkt(rflkt: Rflkt, vars: (String, RflktApi.Val)*) {
    logger.info(s"setRflkt: setting")
    vars foreach {
      case (k, RflktApi.Str(v)) if availableElements(k) =>
        rflkt.setValue(k, v.take(14))
      case (k, RflktApi.Vis(v)) if availableElements(k) =>
        rflkt.setVisisble(k, v)
      case _ =>
    }
  }

  def setRflktPage(page: String, timeout: Option[Int]) =
    getCapRflktReady() foreach { rflkt =>
      val conf = Option(rflkt.getDisplayConfiguration())
      conf.flatMap(c => Option(c.getPage(page))).foreach { p =>
        timeout match {
          case None    => rflkt.sendSetPageIndex(p.getPageIndex())
          case Some(t) => rflkt.sendSetPageIndex(p.getPageIndex(), t)
        }
      }
    }

  def onButtonPressed(fun: String, typ: ButtonPressType) {
    import display.Const.{Function => F}
    (fun, typ) match {
      case (F.backlight, ButtonPressType.SINGLE) =>
        backlight()
      case _ =>
    }
  }

  def onLoadComplete(conf: DisplayConfiguration) {
    availableElements = Set(
      conf.getPages().flatMap(_.getAllElements().map(_.getUpdateKey())): _*)
    getCapRflktReady().foreach(doSetRflkt(_, rflktVars.toSeq: _*))
  }

  private var backlightTimer: Option[CountDownTimer] = None

  private def backlight() {
    backlightTimer.foreach(_.cancel())
    backlightTimer = Some {
      new CountDownTimer(5000, 5000) {
        def onTick(millisLeft: Long) {
          getCapRflkt().foreach(_.sendSetBacklightPercent(100))
        }
        def onFinish() {
          getCapRflkt().foreach(_.sendSetBacklightPercent(0))
          backlightTimer = None
        }
      }.start()
    }
  }

  private val uuid = preferenceVar("uuid", "")
  private def getUuid: UUID = uuid(defaultSharedPreferences) match {
    case s if s.nonEmpty =>
      UUID.fromString(s)
    case "" =>
      val u = UUID.randomUUID()
      uuid(defaultSharedPreferences) = u.toString
      u
  }

  private val lastSensor = preferenceVar("lastSensor", "")
  private def lastSensorOption: Option[ConnectionParams] =
    Option(lastSensor(defaultSharedPreferences))
      .filter(_.nonEmpty).map(ConnectionParams.deserialize)
}

trait ForegroundService extends RService {
  private var stayForeground: Boolean = false

  broadcastReceiver(actionStop) { (context: Context, intent: Intent) =>
    logger.info(s"ForegroundService: actionStop received")
    stopSelf()
  }

  override def onStartCommand(intent: Intent, flags: Int, startId: Int): Int = {
    super.onStartCommand(intent, flags, startId)

    /* Maybe not a good idea if we ever really get killed in a
     * low-memory situation, but then the service doesn't reconnect when
     * restarted so not sticky is fine for now.  A better solution would
     * be to remember what state we should be in and if restarted after
     * a disconnect, just stopSelf(). */
    Service.START_NOT_STICKY
  }

  override def onTaskRemoved(rootIntent: Intent) {
    super.onTaskRemoved(rootIntent)
    stopUnlessForeground()
  }

  private lazy val notificationBuilder = new NotificationCompat.Builder(this)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle(getString(R.string.app_name))
    .setContentText("ready")
    .setContentIntent(pendingMainIntent)
    .addAction(android.R.drawable.ic_menu_delete, "Quit", pendingQuitIntent)

  private lazy val mainIntent = new Intent(this, classOf[Main])

  private lazy val pendingMainIntent =
    PendingIntent.getActivity(this, 0, mainIntent, Intent.FLAG_ACTIVITY_NEW_TASK)

  private lazy val quitIntent = new Intent(actionStop).setPackage(packageName)

  private lazy val pendingQuitIntent =
    PendingIntent.getBroadcast(this, 0, quitIntent, 0)

  protected def startForeground() {
    stayForeground = true
    startForeground(Gen.Id.notification, notificationBuilder.build())
  }

  protected def stopForeground() {
    stayForeground = false
    stopForeground(true)
  }

  protected def stopUnlessForeground() {
    if (!stayForeground)
      stopSelf()
  }

  protected def updateNotification(text: String) {
    getNotificationManager().notify(Gen.Id.notification,
      notificationBuilder.setContentText(text).build())
  }

  private def getNotificationManager(): NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
}
