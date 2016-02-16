package cz.nomi.locusRflktAddon

import org.scaloid.common._

import android.content.Intent

class Main extends SActivity with Log {
  val service = new LocalServiceConnection[MainService]

  onCreate {
    info(s"Main: onCreate")

    contentView = new SVerticalLayout {
      SButton("enable discovery").onClick {
        service(_.enableDiscovery(true))
      }
      SButton("disable discovery").onClick {
        service(_.enableDiscovery(false))
      }
      SButton("connect first").onClick {
        service(_.connectFirst())
      }
      SButton("stop all").onClick {
        stopServices()
        finish()
      }
    }

    startServices()
  }

  private def startServices() {
    startService(new Intent(ctx, classOf[MainService]))
  }

  private def stopServices() {
    stopService(new Intent(ctx, classOf[MainService]))
  }
}

class MainService extends RflktService with LocusService
