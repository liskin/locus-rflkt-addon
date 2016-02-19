package cz.nomi.locusRflktAddon

import scala.collection.JavaConversions._

import org.scaloid.common._

import android.app.{NotificationManager, PendingIntent}
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
import connections.params.ConnectionParams
import connector.listeners.discovery.DiscoveryListener
import connector.capabilities
import capabilities.Capability.CapabilityType
import capabilities.ConfirmConnection
import capabilities.Rflkt
import Rflkt.{ButtonPressType, LoadConfigResult}
import connector.HardwareConnectorEnums.{SensorConnectionError, SensorConnectionState}
import com.wahoofitness.common.display
import display.{DisplayConfiguration, DisplayButtonPosition}

trait RflktApi {
  def enableDiscovery(enable: Boolean): Unit
  def connectFirst(): Unit
  def setRflkt(vars: (String, String)*): Unit
}

trait RflktService extends LocalService with Log with RflktApi
{ this: LocusApi =>

  import RflktService._

  private var hwCon: HardwareConnector = null

  onCreate {
    info(s"RflktService: onCreate")
    hwCon = new HardwareConnector(ctx, Hardware)
    startForeground()
  }

  onDestroy {
    info(s"RflktService: onDestroy")
    hwCon.stopDiscovery()
    hwCon.shutdown()
  }

  override def onTaskRemoved(rootIntent: Intent) {
    super.onTaskRemoved(rootIntent)

    if (curSensor.isEmpty)
      stopSelf()
  }

  private lazy val notificationBuilder = new NotificationCompat.Builder(ctx)
    .setSmallIcon(R.drawable.icon)
    .setContentTitle("Locus Wahoo RFLKT addon")
    .setContentText("ready")
    .setContentIntent(pendingMainIntent)
    // TODO: add some actions, e.g. disconnect

  private lazy val mainIntent = new Intent(ctx, classOf[Main])

  private lazy val pendingMainIntent =
    PendingIntent.getActivity(ctx, 0, mainIntent, Intent.FLAG_ACTIVITY_NEW_TASK)

  private def startForeground() {
    startForeground(notificationId, notificationBuilder.build())
  }

  private def updateNotification(text: String) {
    getNotificationManager().notify(notificationId,
      notificationBuilder.setContentText(text).build())
  }

  private def getNotificationManager(): NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

  private object Hardware extends HardwareConnector.Listener {
    // FIXME: deprecated, use onSensorConnectionStateChanged instead
    override def connectedSensor(s: SensorConnection) {
      info(s"connectedSensor: $s")
      curSensor = Some(s)
      lastSensor() = s.getConnectionParams.serialize

      getCapConfirm().get.addListener(Confirmation)
      getCapRflkt().get.addListener(RFLKT)

      requestConfirmation()
    }

    // FIXME: deprecated, use onSensorConnectionStateChanged instead
    override def disconnectedSensor(s: SensorConnection) {
      info(s"disconnectedSensor: $s")
      curSensor = None
    }

    override def connectorStateChanged(nt: NetworkType, state: HardwareConnectorState) {
      info(s"connectorStateChanged: $nt, $state")
    }

    override def onFirmwareUpdateRequired(s: SensorConnection, current: String, recommended: String) {
      info(s"onFirmwareUpdateRequired: $s, $current, $recommended")
    }
  }

  private object Discovery extends DiscoveryListener {
    override def onDeviceDiscovered(params: ConnectionParams) {
      if (!params.hasCapability(CapabilityType.Rflkt))
        return

      info(s"onDeviceDiscovered: $params")
      toast(s"discovered: ${params.getName}")
    }

    override def onDiscoveredDeviceLost(params: ConnectionParams) {
      if (!params.hasCapability(CapabilityType.Rflkt))
        return

      info(s"onDiscoveredDeviceLost: $params")
      toast(s"lost: ${params.getName}")
    }

    override def onDiscoveredDeviceRssiChanged(params: ConnectionParams, rssi: Int) {
      info(s"onDiscoveredDeviceRssiChanged: $params, $rssi")
    }
  }

  private object Connection extends SensorConnection.Listener {
    override def onNewCapabilityDetected(s: SensorConnection, typ: CapabilityType) {
      info(s"onNewCapabilityDetected: $s, $typ")
    }

    override def onSensorConnectionError(s: SensorConnection, e: SensorConnectionError) {
      info(s"onSensorConnectionError: $s, $e")
      toast(s"${s.getDeviceName}: $e")
    }

    override def onSensorConnectionStateChanged(s: SensorConnection, state: SensorConnectionState) {
      info(s"onSensorConnectionStateChanged: $s, $state")
      toast(s"${s.getDeviceName}: $state")
      updateNotification(s"$state")
    }
  }

  private object Confirmation extends ConfirmConnection.Listener {
    override def onConfirmationProcedureStateChange(state: ConfirmConnection.State, error: ConfirmConnection.Error) {
      info(s"onConfirmationProcedureStateChange: $state, $error")
      if (state == ConfirmConnection.State.FAILED) {
        requestConfirmation()
      }
    }

    override def onUserAccept() {
      info(s"onUserAccept")
      loadConfig()
      getCapRflkt().foreach(_.sendSetBacklightPercent(0))
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
        info(s"onButtonPressed: $pos, $fun, $typ")
        (fun, typ) match {
          case ("PAGE_RIGHT", ButtonPressType.SINGLE) =>
            rflkt.sendShowNextPage()
          case ("PAGE_LEFT", ButtonPressType.SINGLE) =>
            rflkt.sendShowPreviousPage()
          case ("START_STOP_WORKOUT", ButtonPressType.SINGLE) =>
            toggleRecording()
          case ("BACKLIGHT", ButtonPressType.SINGLE) =>
            backlight()
          case _ =>
        }
      }
    }
    override def onColorInvertedReceived(inverted: Boolean) {}
    override def onConfigVersionsReceived(ver: Array[Int]) {}
    override def onDateReceived(date: java.util.Calendar) {}
    override def onDisplayOptionsReceived(x1: DisplayDateFormat, x2: DisplayTimeFormat, x3: DisplayDayOfWeek, x4: DisplayWatchFaceStyle) {}
    override def onLoadComplete() {
      info(s"onLoadComplete")
    }
    override def onLoadFailed(result: LoadConfigResult) {
      info(s"onLoadFailed: $result")
    }
    override def onLoadProgressChanged(progress: Int) {
      info(s"onLoadProgressChanged: $progress")
    }
    override def onPageIndexReceived(index: Int) {}
  }

  def enableDiscovery(enable: Boolean) {
    enableBluetooth()
    enable match {
      case true =>
        hwCon.startDiscovery(Discovery)
      case false =>
        hwCon.stopDiscovery()
    }
  }

  def connectFirst() {
    enableBluetooth()
    val rflkts = hwCon.getDiscoveredConnectionParams()
      .filter(_.hasCapability(CapabilityType.Rflkt))
    val params = rflkts.headOption orElse lastSensorOption
    params match {
      case Some(p) =>
        hwCon.requestSensorConnection(p, Connection)
        hwCon.stopDiscovery()
      case None => toast("no sensor to connect to")
    }
  }

  private def enableBluetooth() {
    if (hwCon.getHardwareConnectorState(NetworkType.BTLE) == HardwareConnectorState.HARDWARE_NOT_ENABLED) {
      val intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      startActivity(intent)
    }
  }

  private def getCap[T](typ: CapabilityType): Option[T] =
    curSensor.map(_.getCurrentCapability(typ).asInstanceOf[T]).filter(_ != null)

  private def getCapConfirm(): Option[ConfirmConnection] =
    getCap(CapabilityType.ConfirmConnection)

  private def getCapRflkt(): Option[Rflkt] =
    getCap(CapabilityType.Rflkt)

  private def requestConfirmation() {
    getCapConfirm() foreach {
      _.requestConfirmation(ConfirmConnection.Role.MASTER, "Locus", getUuid, "LocusRflktAddon")
    }
  }

  private def loadConfig() {
    val config = DisplayConfiguration.fromRawResource(getResources, R.raw.display_cfg_rflkt_default)
    getCapRflkt() foreach {
      _.loadConfig(config)
    }
  }

  def setRflkt(vars: (String, String)*) {
    info(s"setRflkt: $vars")
    getCapRflkt() foreach { rflkt =>
      if (rflkt.getLastLoadConfigResult() == LoadConfigResult.SUCCESS) {
        info(s"setRflkt: doing setValues")
        vars foreach { case (k, v) =>
          rflkt.setValue(k, v.take(14))
        }
      }
    }
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

  private val uuid = preferenceVar("")
  private def getUuid: UUID = uuid() match {
    case s if s.nonEmpty =>
      UUID.fromString(s)
    case "" =>
      val u = UUID.randomUUID()
      uuid() = u.toString
      u
  }

  private val lastSensor = preferenceVar("")
  private def lastSensorOption: Option[ConnectionParams] =
    Option(lastSensor()) filter (_.nonEmpty) map (ConnectionParams.deserialize)

  private var curSensor: Option[SensorConnection] = None
}

object RflktService {
  private val notificationId: Int = 1 // unique within app
}
