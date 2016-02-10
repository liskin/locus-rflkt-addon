package cz.nomi.locusRflktAddon

import scala.collection.JavaConversions._

import org.scaloid.common._

import android.content.Intent

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
import connector.HardwareConnectorEnums.{SensorConnectionError, SensorConnectionState}

class HardwareConnectorService extends LocalService with Log {
  import HardwareConnectorService._

  private var hwCon: HardwareConnector = null

  onCreate {
    info(s"HardwareConnectorService: onCreate")
    hwCon = new HardwareConnector(ctx, Callback)
  }

  onDestroy {
    info(s"HardwareConnectorService: onDestroy")
    hwCon.stopDiscovery(networkType)
    hwCon.shutdown()
  }

  override def onTaskRemoved(rootIntent: Intent) {
    if (curSensor.isEmpty)
      stopSelf()
  }

  private object Callback extends HardwareConnector.Callback {
    def connectedSensor(s: SensorConnection): Unit = {
      info(s"connectedSensor: $s")
      curSensor = Some(s)
      lastSensor() = s.getConnectionParams.serialize
    }

    def disconnectedSensor(s: SensorConnection): Unit = {
      info(s"disconnectedSensor: $s")
      curSensor = None
    }

    def connectorStateChanged(nt: NetworkType, state: HardwareConnectorState): Unit = {
      info(s"connectorStateChanged: $nt, $state")
    }

    def hasData(): Unit = {
      info(s"hasData")
    }

    def onFirmwareUpdateRequired(s: SensorConnection, current: String, recommended: String): Unit = {
      info(s"onFirmwareUpdateRequired: $s, $current, $recommended")
    }
  }

  private object Discovery extends DiscoveryListener {
    def onDeviceDiscovered(params: ConnectionParams): Unit = {
      info(s"onDeviceDiscovered: $params")
      toast(s"discovered: ${params.getName}")
    }

    def onDiscoveredDeviceLost(params: ConnectionParams): Unit = {
      info(s"onDiscoveredDeviceLost: $params")
      toast(s"lost: ${params.getName}")
    }

    def onDiscoveredDeviceRssiChanged(params: ConnectionParams, rssi: Int) = {
      info(s"onDiscoveredDeviceRssiChanged: $params, $rssi")
    }
  }

  private object Connection extends SensorConnection.Listener {
    def onNewCapabilityDetected(s: SensorConnection, typ: CapabilityType): Unit = {
      info(s"onNewCapabilityDetected: $s, $typ")

      s.getCurrentCapability(typ) match {
        case confirm: ConfirmConnection =>
          confirm.requestConfirmation(ConfirmConnection.Role.MASTER, "Locus", getUuid, "LocusRflktAddon")
        case _ =>
      }
    }

    def onSensorConnectionError(s: SensorConnection, e: SensorConnectionError): Unit = {
      info(s"onSensorConnectionError: $s, $e")
      toast(s"${s.getDeviceName}: $e")
    }

    def onSensorConnectionStateChanged(s: SensorConnection, state: SensorConnectionState): Unit = {
      info(s"onSensorConnectionStateChanged: $s, $state")
      toast(s"${s.getDeviceName}: $state")
    }
  }

  def enableDiscovery(enable: Boolean): Unit = enable match {
    case true =>
      hwCon.startDiscovery(sensorType, networkType, Discovery)
    case false =>
      hwCon.stopDiscovery(networkType)
  }

  def connectFirst(): Unit = {
    val params = hwCon.getDiscoveredConnectionParams(networkType, sensorType).headOption orElse lastSensorOption
    params match {
      case Some(p) =>
        hwCon.requestSensorConnection(p, Connection)
        hwCon.stopDiscovery(networkType)
      case None => toast("no sensor to connect to")
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

object HardwareConnectorService {
  private val sensorType = SensorType.DISPLAY
  private val networkType = NetworkType.BTLE
}
