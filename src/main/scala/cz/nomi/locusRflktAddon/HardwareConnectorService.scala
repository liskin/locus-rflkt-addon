package cz.nomi.locusRflktAddon

import scala.collection.JavaConversions._

import org.scaloid.common._

import com.wahoofitness.connector
import connector.HardwareConnector
import connector.HardwareConnectorTypes.{NetworkType, SensorType}
import connector.HardwareConnectorEnums.HardwareConnectorState
import connector.conn.connections
import connections.SensorConnection
import connections.params.ConnectionParams
import connector.listeners.discovery.DiscoveryListener
import connector.capabilities.Capability.CapabilityType
import connector.HardwareConnectorEnums.{SensorConnectionError, SensorConnectionState}

class HardwareConnectorService extends LocalService with Log {
  import HardwareConnectorService._

  private var hwCon: HardwareConnector = null

  onCreate {
    hwCon = new HardwareConnector(ctx, Callback)
  }

  onDestroy {
    hwCon.stopDiscovery(networkType)
    hwCon.shutdown()
  }

  private object Callback extends HardwareConnector.Callback {
    def connectedSensor(s: SensorConnection): Unit = {
      info(s"connectedSensor: $s")
      // TODO: save it somewhere and connect to it next time
    }

    def disconnectedSensor(s: SensorConnection): Unit = {
      info(s"disconnectedSensor: $s")
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

  def connectAll(): Unit = {
    for (params <- hwCon.getDiscoveredConnectionParams(networkType, sensorType)) {
      hwCon.requestSensorConnection(params, Connection)
    }
  }
}

object HardwareConnectorService {
  private val sensorType = SensorType.DISPLAY
  private val networkType = NetworkType.BTLE
}
