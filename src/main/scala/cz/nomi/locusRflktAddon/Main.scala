package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.Intent

class Main extends SActivity with Log {
  val hwCon = new LocalServiceConnection[HardwareConnectorService]
  val locus = new LocalServiceConnection[LocusService]

  onCreate {
    info(s"Main: onCreate")

    contentView = new SVerticalLayout {
      SButton("enable discovery").onClick {
        hwCon(_.enableDiscovery(true))
      }
      SButton("disable discovery").onClick {
        hwCon(_.enableDiscovery(false))
      }
      SButton("connect first").onClick {
        hwCon(_.connectFirst())
      }
      SButton("stop services").onClick {
        stopServices()
      }
    }

    startServices()
  }

  private def startServices() {
    startService(new Intent(ctx, classOf[HardwareConnectorService]))
    startService(new Intent(ctx, classOf[LocusService]))
  }

  private def stopServices() {
    stopService(new Intent(ctx, classOf[HardwareConnectorService]))
    stopService(new Intent(ctx, classOf[LocusService]))
  }
}
