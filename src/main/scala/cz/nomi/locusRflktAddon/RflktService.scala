package cz.nomi.locusRflktAddon

import scala.collection.JavaConversions._

import org.scaloid.common._

import android.app.{NotificationManager, PendingIntent}
import android.content.{Intent, Context}
import android.support.v4.app.NotificationCompat

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
    hwCon = new HardwareConnector(ctx, Callback)
    startForeground()
  }

  onDestroy {
    info(s"RflktService: onDestroy")
    hwCon.stopDiscovery(networkType)
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

  private object Callback extends HardwareConnector.Callback {
    def connectedSensor(s: SensorConnection) {
      info(s"connectedSensor: $s")
      curSensor = Some(s)
      lastSensor() = s.getConnectionParams.serialize

      getCapConfirm().get.addListener(Confirmation)
      getCapRflkt().get.addListener(RFLKT)

      requestConfirmation()
    }

    def disconnectedSensor(s: SensorConnection) {
      info(s"disconnectedSensor: $s")
      curSensor = None
    }

    def connectorStateChanged(nt: NetworkType, state: HardwareConnectorState) {
      info(s"connectorStateChanged: $nt, $state")
    }

    def hasData() {}

    def onFirmwareUpdateRequired(s: SensorConnection, current: String, recommended: String) {
      info(s"onFirmwareUpdateRequired: $s, $current, $recommended")
    }
  }

  private object Discovery extends DiscoveryListener {
    def onDeviceDiscovered(params: ConnectionParams) {
      info(s"onDeviceDiscovered: $params")
      toast(s"discovered: ${params.getName}")
    }

    def onDiscoveredDeviceLost(params: ConnectionParams) {
      info(s"onDiscoveredDeviceLost: $params")
      toast(s"lost: ${params.getName}")
    }

    def onDiscoveredDeviceRssiChanged(params: ConnectionParams, rssi: Int) {
      info(s"onDiscoveredDeviceRssiChanged: $params, $rssi")
    }
  }

  private object Connection extends SensorConnection.Listener {
    def onNewCapabilityDetected(s: SensorConnection, typ: CapabilityType) {
      info(s"onNewCapabilityDetected: $s, $typ")
    }

    def onSensorConnectionError(s: SensorConnection, e: SensorConnectionError) {
      info(s"onSensorConnectionError: $s, $e")
      toast(s"${s.getDeviceName}: $e")
    }

    def onSensorConnectionStateChanged(s: SensorConnection, state: SensorConnectionState) {
      info(s"onSensorConnectionStateChanged: $s, $state")
      toast(s"${s.getDeviceName}: $state")
      updateNotification(s"$state")
    }
  }

  private object Confirmation extends ConfirmConnection.Listener {
    def onConfirmationProcedureStateChange(state: ConfirmConnection.State, error: ConfirmConnection.Error) {
      info(s"onConfirmationProcedureStateChange: $state, $error")
      if (state == ConfirmConnection.State.FAILED) {
        requestConfirmation()
      }
    }

    def onUserAccept() {
      info(s"onUserAccept")
      loadConfig()
    }
  }

  private object RFLKT extends Rflkt.Listener {
    import connector.packets.dcp.response.DCPR_DateDisplayOptionsPacket._

    def onAutoPageScrollRecieved() {}
    def onBacklightPercentReceived(p: Int) {}
    def onButtonPressed(pos: DisplayButtonPosition, typ: ButtonPressType) {
      getCapRflkt() foreach { rflkt =>
        val buttonCfg = rflkt.getDisplayConfiguration().getButtonCfg()
        (buttonCfg.getButtonFunction(pos), typ) match {
          case ("PAGE_RIGHT", ButtonPressType.SINGLE) =>
            switchPage(rflkt, 1)
          case ("PAGE_LEFT", ButtonPressType.SINGLE) =>
            switchPage(rflkt, -1)
          case _ =>
        }
      }
    }
    def onButtonStateChanged(pos: DisplayButtonPosition, pressed: Boolean) {}
    def onColorInvertedReceived(inverted: Boolean) {}
    def onConfigVersionsReceived(ver: Array[Int]) {}
    def onDateReceived(date: java.util.Calendar) {}
    def onDisplayOptionsReceived(x1: DisplayDateFormat, x2: DisplayTimeFormat, x3: DisplayDayOfWeek, x4: DisplayWatchFaceStyle) {}
    def onLoadComplete() {
      info(s"onLoadComplete")
    }
    def onLoadFailed(result: LoadConfigResult) {
      info(s"onLoadFailed: $result")
    }
    def onLoadProgressChanged(progress: Int) {
      info(s"onLoadProgressChanged: $progress")
    }
    def onPageIndexReceived(index: Int) {}
    def onSleepOnDisconnectReceived(state: Boolean) {}

    private def switchPage(rflkt: Rflkt, offset: Int) {
      val cfg = rflkt.getDisplayConfiguration()
      val visiblePages = cfg.getVisiblePages()
      val countPages = visiblePages.size()

      if (countPages > 1) {
        val page = rflkt.getPage()
        val visibleIndex = visiblePages.indexOf(page)
        val wantedIndex = ((visibleIndex + offset) % countPages + countPages) % countPages
        rflkt.sendSetPageIndex(visiblePages(wantedIndex).getPageIndex())
      }
    }
  }

  def enableDiscovery(enable: Boolean): Unit = enable match {
    case true =>
      hwCon.startDiscovery(sensorType, networkType, Discovery)
    case false =>
      hwCon.stopDiscovery(networkType)
  }

  def connectFirst() {
    val params = hwCon.getDiscoveredConnectionParams(networkType, sensorType).headOption orElse lastSensorOption
    params match {
      case Some(p) =>
        hwCon.requestSensorConnection(p, Connection)
        hwCon.stopDiscovery(networkType)
      case None => toast("no sensor to connect to")
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
          // XXX: check if take(15) really is the right thing to do
          rflkt.setValue(k, v.take(15))
        }
      }
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
    Option(lastSensor()) filter (_.nonEmpty) map (ConnectionParams.fromString)

  private var curSensor: Option[SensorConnection] = None
}

object RflktService {
  private val sensorType = SensorType.DISPLAY
  private val networkType = NetworkType.BTLE
  private val notificationId: Int = 1 // unique within app
}
